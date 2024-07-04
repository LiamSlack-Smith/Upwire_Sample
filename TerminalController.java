package controllers;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import dao.contract.Contract;
import dao.counterparties.Counterparty;
import dao.terminal.Terminal;
import dao.tradingFirm.TradingFirm;
import dao.user.User;
import enums.ResponseErrorCode;
import org.bson.types.ObjectId;
import play.core.j.JavaResults;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import processing.terminal.ITerminalProcessor;
import processing.tradingFirm.ITradingFirmProcessor;
import processing.user.IUserProcessor;
import requests.CreateTerminalRequest;
import requests.UpdateTerminalRequest;
import response.ErrorResponse;
import response.PageInfo;
import response.TerminalResponse;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.*;

@Security.Authenticated(Secured.class)
public class TerminalController extends Controller {
    private final ITerminalProcessor terminalProcessor;
    private final ITradingFirmProcessor tradingFirmProcessor;
    private final IUserProcessor userProcessor;

    @Inject
    public TerminalController(ITerminalProcessor terminalProcessor, IUserProcessor userProcessor, ITradingFirmProcessor tradingFirmProcessor) {
        this.terminalProcessor = terminalProcessor;
        this.userProcessor = userProcessor;
        this.tradingFirmProcessor = tradingFirmProcessor;
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result createTerminal() throws ParseException {
        if (checkIsAdmin(request().username())) {
            CreateTerminalRequest request = Json.fromJson(request().body().asJson(), CreateTerminalRequest.class);
            if (checkDuplicates(request.symbol, null)) {
                return badRequest(Json.toJson(new ErrorResponse(ResponseErrorCode.DUPLICATE_CONTRACT)));
            }

            ObjectId terminalId = terminalProcessor.create(request);
            return ok(Json.toJson(terminalId));
        } else {
            return new StatusHeader(JavaResults.MethodNotAllowed());
        }
    }

    public Result getAllTerminals(String symbol, String showUser, String pageNum, String pageLength) throws ParseException {
        if (checkIsAdmin(request().username())) {
            ISO8601DateFormat df = new ISO8601DateFormat();

            Integer pgNum = pageNum.isEmpty() ? null : Integer.parseInt(pageNum);
            Integer pgLength = pageLength.isEmpty() ? null : Integer.parseInt(pageLength);

            List<Terminal> terminals = symbol == null ? terminalProcessor.getAll() : terminalProcessor.getAll();

            Collections.sort(terminals, new Comparator<Terminal>() {
                @Override
                public int compare(Terminal t1, Terminal t2) {
                    return t1.getSymbol().compareTo(t2.getSymbol());
                }
            });

            return ok(Json.toJson(pageListTerminals(terminals, pgNum, pgLength)));
        } else {
            return new StatusHeader(JavaResults.MethodNotAllowed());
        }
    }

    public Result getActiveTerminals(String expiryDate) throws ParseException {

        ISO8601DateFormat df = new ISO8601DateFormat();
        Date date = expiryDate.isEmpty() ? null : df.parse(expiryDate);
        List<Terminal> terminals = date == null ? terminalProcessor.getAllActive(null) : terminalProcessor.getAllActive(date);

        Collections.sort(terminals, new Comparator<Terminal>() {
            @Override
            public int compare(Terminal t1, Terminal t2) {
                return t1.getSymbol().compareTo(t2.getSymbol());
            }
        });

        return ok(Json.toJson(terminals));
    }

    public Result getTerminals(String symbol) throws ParseException {

        List<Terminal> terminals = terminalProcessor.getTerminals();

        Collections.sort(terminals, new Comparator<Terminal>() {
            @Override
            public int compare(Terminal t1, Terminal t2) {
                return t1.getSymbol().compareTo(t2.getSymbol());
            }
        });

        return ok(Json.toJson(terminals));
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result updateTerminal() throws ParseException {
        if (checkIsAdmin(request().username())) {
            UpdateTerminalRequest request = Json.fromJson(request().body().asJson(), UpdateTerminalRequest.class);
            if (checkDuplicates(request.symbol, request.id)) {
                return badRequest(Json.toJson(new ErrorResponse(ResponseErrorCode.DUPLICATE_CONTRACT)));
            }

            terminalProcessor.update(request);
            return ok();
        } else {
            return new StatusHeader(JavaResults.MethodNotAllowed());
        }
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result deleteTerminal() throws ParseException {
        if (checkIsAdmin(request().username())) {
            JsonNode json = request().body().asJson();
            String id = json.findPath("id").asText();


            terminalProcessor.remove(id);
            return ok();
        } else {
            return new StatusHeader(JavaResults.MethodNotAllowed());
        }
    }


    @BodyParser.Of(BodyParser.Json.class)
    public Result linkTerminal()
    {
        JsonNode json = request().body().asJson();
        String tradingFirmId = json.findPath("tradingFirmId").asText();
        String terminalId = json.findPath("terminalId").asText();

        TradingFirm tradingFirm = tradingFirmProcessor.getById(tradingFirmId);

        if(tradingFirm.getTerminalIds() != null && tradingFirm.getTerminalIds().contains(terminalId)) return ok();

        tradingFirm.addTerminalId(terminalId);

        Terminal terminal = terminalProcessor.getTerminalByNumber(terminalId);

        terminal.setTradingFirmId(new ObjectId(tradingFirmId));

        terminal.setStatus("Active");

        terminalProcessor.save(terminal);

        tradingFirmProcessor.save(tradingFirm);

        return ok(Json.toJson(tradingFirm.getTerminalIds()));
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result unlinkTerminal(){

        JsonNode json = request().body().asJson();
        String terminalId = json.findPath("terminalId").asText();

        Terminal terminal = terminalProcessor.getTerminalByNumber(terminalId);

        TradingFirm tradingFirm = tradingFirmProcessor.getById(terminal.getTradingFirmId().toString());

        if(terminal.getTradingFirmId()!=null){
            List<String> newList = new ArrayList<>();
            for(String id : tradingFirm.getTerminalIds()){
                if(!id.equals(terminalId)) newList.add(id);
            }
            tradingFirm.setTerminalIds(newList);
        }

        terminal.setStatus("Vacant");

        terminalProcessor.save(terminal);

        tradingFirmProcessor.save(tradingFirm);

        return ok(Json.toJson(tradingFirm.getTerminalIds()));
    }

    public Result getTerminalsByTradingFirm(String tradingFirmId) throws ParseException {

        List<Terminal> terminals = new ArrayList<>();

        TradingFirm tradingFirm = tradingFirmProcessor.getById(tradingFirmId);

        if(tradingFirm.getTerminalIds() == null) return ok();

        List<String> terminalIdList = tradingFirm.getTerminalIds();

        for(String terminalId : terminalIdList){
            Terminal terminal = terminalProcessor.getTerminalByNumber(terminalId);
            terminals.add(terminal);
        }

        return ok(Json.toJson(terminals));
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result terminalSearch() throws Exception {

        JsonNode json = request().body().asJson();

        String message = json.findPath("messageBody").asText();

        //Extract values for a terminal search from the user input
        JsonObject responseObject = terminalProcessor.GPTSearch(message);

        //Receive a response string containing results for the user's search
        String response = terminalProcessor.elasticSearch(responseObject);

        return ok(response);
    }
    @BodyParser.Of(BodyParser.Json.class)
    public Result terminalStatSearch() throws Exception {
        JsonNode json = request().body().asJson();

        String message = json.findPath("messageBody").asText();

        //Extract values for a terminal search from the user input
        JsonObject responseObject = terminalProcessor.GPTSearch(message);

        //Receive a response string containing results for the user's search
        String response = terminalProcessor.terminalStatSearch(responseObject);

        return ok(response);
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result getNearbyTerminals() throws Exception {
        JsonNode json = request().body().asJson();

        String latitude = json.findPath("lat").asText();
        String longitude = json.findPath("lon").asText();

        JsonObject locationJson = new JsonObject();

        locationJson.addProperty("lat", latitude);
        locationJson.addProperty("lon", longitude);

        //Receive a response string containing a list of terminals near the location
        String nearbyTerminals = terminalProcessor.getTerminalsInRange(locationJson, "100km");

        return ok(nearbyTerminals);
    }

    private boolean checkIsAdmin(String username) {
        User user = userProcessor.getUserById(username);
        return user.isAdministrator();
    }


    private boolean checkDuplicates(String symbol, String terminalId) {
        List<Terminal> terminals =  terminalProcessor.getAll(null);

        if (terminalId == null)
        {
            return terminals.stream().anyMatch(c -> c.getSymbol().contentEquals(symbol));
        } else {
            return terminals.stream().anyMatch(c -> c.getSymbol().contentEquals(symbol)&& !c.getId().toString().contentEquals(terminalId));
        }
    }

    /**
     * Paginate contract list
     * @param list all list
     * @param pageNum page number
     * @param pageLength records count on page
     * @return paginated list
     */
    public TerminalResponse pageListTerminals(List<Terminal> list, Integer pageNum, Integer pageLength) {
        int totalItems = list.size();

        if (pageNum == null || pageLength == null) {
            return new TerminalResponse(list, new PageInfo(1, 1, totalItems, totalItems));
        }

        int startLine = pageLength * (pageNum - 1);
        int endLine = pageLength * pageNum;

        List<Terminal> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            if (i >= startLine && i < endLine) {
                result.add(list.get(i));
            }
        }
        int pageCount = totalItems % pageLength == 0 ? totalItems/pageLength : totalItems/pageLength + 1;
        return new TerminalResponse(result, new PageInfo(pageNum, pageCount, pageLength, totalItems));
    }
}
