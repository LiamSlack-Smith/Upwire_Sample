package seleniumTest;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.support.ui.Select;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class AccountTests{
    private WebDriver driver;
    private final StringBuffer verificationErrors = new StringBuffer();

    @BeforeTest
    @Parameters("browser")
    public void setUp(String browser) throws Exception {
        if(browser.equalsIgnoreCase("firefox")){
            //Create options for the web driver (headless mode, size etc.)
            FirefoxOptions options = new FirefoxOptions();
            options.addArguments("--headless","--window-size=1920,1080", "--ignore-certificate-errors", "--disable-extensions", "--no-sandbox", "--disable-dev-shm-usage");
            //Instantiate web driver with the options enabled
            WebDriverManager.firefoxdriver().setup();
            driver = new FirefoxDriver(options);
        }
        else if (browser.equalsIgnoreCase("chrome")) {
            //Create options for the web driver (headless mode, size etc.)
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless","--window-size=1920,1080", "--ignore-certificate-errors", "--disable-extensions", "--no-sandbox", "--disable-dev-shm-usage");
            //Instantiate web driver with the options enabled
            WebDriverManager.chromedriver().setup();
            driver = new ChromeDriver(options);
        }
        else if(browser.equalsIgnoreCase("edge")){
            //Create options for the web driver (headless mode, size etc.)
            EdgeOptions options = new EdgeOptions();
            options.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);
            List<String> args = Arrays.asList("--headless", "--window-size=1920,1080", "--ignore-certificate-errors", "--disable-extensions", "--no-sandbox", "--disable-dev-shm-usage");
            Map<String, Object> map = new HashMap<>();
            map.put("args",args);
            options.setCapability("ms:edgeOptions", map);
            //Instantiate web driver with the options enabled
            WebDriverManager.edgedriver().setup();
            driver = new EdgeDriver(options);
        }
        //Wait in case anything goes wrong and needs time to load, if it still fails the test will fail
        driver.manage().timeouts().implicitlyWait(50, TimeUnit.MILLISECONDS);
    }

    @Test (priority = 1)
    public void invalidLogin() throws Exception {
        driver.get("http://localhost:9000");
        Reporter.log("Entering invalid username");
        driver.findElement(By.xpath("//input[@name='username']")).sendKeys("FakeUsername");
        Reporter.log("Entering invalid password");
        driver.findElement(By.xpath("//input[@name='password']")).sendKeys("FakePassword");
        driver.findElement(By.id("submitButton")).click();
        try {
            assertTrue(isElementPresent(By.cssSelector("div.alert.alert-danger")));
        } catch (Error e) {
            verificationErrors.append(e.toString());
        }
        Reporter.log("Invalid username and/or password alert present and user cannot login");
    }

    @Test (priority = 2)
    public void loginAsTrader() throws Exception{
        driver.get("http://localhost:9000/login");
        Reporter.log("Entering trader username");
        driver.findElement(By.xpath("//input[@name='username']")).sendKeys("testtrader1");
        Reporter.log("Entering trader password");
        driver.findElement(By.xpath("//input[@name='password']")).sendKeys("testtrader1");
        driver.findElement(By.id("submitButton")).click();
        Reporter.log("Successful trader login");
    }

    @Test (priority = 2)
    public void traderInvalidPasswordWeak() throws Exception {
        driver.get("http://localhost:9000");
        driver.findElement(By.xpath("//button[@type='button']")).click();
        Reporter.log("Navigating to password change");
        driver.findElement(By.linkText("Settings")).click();
        //Input real password in old password field
        Reporter.log("Inputting current password");
        driver.findElement(By.xpath("//input[@type='text']")).sendKeys("testtrader1");
        //Input weak password in new password field
        Reporter.log("Inputting weak new password");
        driver.findElement(By.xpath("//div[3]/div/input")).sendKeys("weakpassword");
        //Check that the error is present
        assertTrue(isElementPresent(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Change password'])[1]/following::label[1]")));
        Reporter.log("Alert informing user password is too weak is present");
    }

    @Test (priority = 2)
    public void traderWrongOldPassChange() throws Exception{
        //Reset to start page
        driver.get("http://localhost:9000");
        driver.findElement(By.xpath("//button[@type='button']")).click();
        Reporter.log("Navigating to password change");
        driver.findElement(By.linkText("Settings")).click();
        //Input incorrect old password
        Reporter.log("Inputting incorrect old password");
        driver.findElement(By.xpath("//input[@type='text']")).sendKeys("wrongpassword");
        //Input anything for new password
        Reporter.log("Inputting random characters for new password");
        driver.findElement(By.xpath("//div[3]/div/input")).sendKeys("Abcd");
        //Check that the error is present
        assertTrue(isElementPresent(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Change password'])[1]/following::label[1]")));
        Reporter.log("Alert informing user old password provided was incorrect");
    }

    @Test (priority = 2)
    public void adminAddAccountToTrader()throws Exception {
        adminLogin();
        driver.get("http://localhost:9000/auctions/active");
        Reporter.log("Navigating to trader panel");
        driver.findElement(By.xpath("//button[@type='button']")).click();
        driver.findElement(By.linkText("Admin")).click();
        driver.findElement(By.linkText("Traders")).click();
        Reporter.log("Adding account 'traderacc1'");
        driver.findElement(By.linkText("Accounts")).click();
        driver.findElement(By.xpath("//*/text()[normalize-space(.)='Create New Account']/parent::*")).click();
        driver.findElement(By.xpath("//input[@type='text']")).sendKeys("traderacc1");
        driver.findElement(By.xpath("//span")).click();
        driver.findElement(By.xpath("//button[@type='button']")).click();
        Reporter.log("Account added to trader successful");
    }

    @Test (priority = 2)
    public void adminAddTradingFirmToTrader(){
        adminLogin();
        driver.get("http://localhost:9000/auctions");
        driver.findElement(By.xpath("//span")).click();
        driver.findElement(By.linkText("Admin")).click();
        driver.findElement(By.linkText("Traders")).click();
        driver.findElement(By.xpath("//td[10]/span")).click();
        new Select(driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Trading Firm'])[1]/following::select[1]"))).selectByVisibleText("TestTradingFirm");
        driver.findElement(By.xpath("//button[@type='button']")).click();
        driver.findElement(By.linkText("Traders")).click();
        if(driver.getPageSource().contains("TestTradingFirm"))
        {
            Reporter.log("Successfully added trading firm to trader");
        }
        else
        {
            org.testng.Assert.fail("Adding trading firm to trader unsuccessful");
        }

    }

    @Test (priority = 2)
    public void adminAddPublisherCapabilityToTrader(){
        adminLogin();
        driver.get("http://localhost:9000/auctions");
        driver.findElement(By.xpath("//span")).click();
        driver.findElement(By.linkText("Admin")).click();
        driver.findElement(By.linkText("Traders")).click();
        driver.findElement(By.xpath("//td[10]/span")).click();
        new Select(driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Trading Firm'])[1]/following::select[1]"))).selectByVisibleText("TestTradingFirm");
        driver.findElement(By.xpath("//button[@type='button']")).click();
        logout();
    }

    @Test (priority = 3)
    public void createBuyListing() throws Exception {
        loginAsTrader();
        driver.get("http://localhost:9000/auctions");
        driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Completed'])[1]/following::button[1]")).click();
        driver.findElement(By.xpath("//input[@name='optradio2']")).click();
        driver.findElement(By.xpath("//div[@id='listingTab']/div[5]")).click();
        driver.findElement(By.xpath("//input[@type='number']")).clear();
        driver.findElement(By.xpath("//input[@type='number']")).sendKeys("1250");
        driver.findElement(By.xpath("//div[@id='listingTab']/div[6]/div/input")).clear();
        driver.findElement(By.xpath("//div[@id='listingTab']/div[6]/div/input")).sendKeys("250");
        int x = 0;
        //Only does it 49 times because the value starts at 0.01 and we're going up to 0.50 for price
        while(x <= 48){
            driver.findElement(By.name("price")).sendKeys(Keys.UP);
            x++;
        }
        driver.findElement(By.linkText("Terminal Details")).click();
        new Select(driver.findElement(By.xpath("//div[@id='browseTab']/div[4]/label[2]/select"))).selectByVisibleText("Oceania");
        new Select(driver.findElement(By.xpath("//div[@id='browseTab']/div[5]/label[2]/select"))).selectByVisibleText("Australia");
        new Select(driver.findElement(By.xpath("//div[@id='browseTab']/div[6]/label[2]/select"))).selectByVisibleText("Sydney");
        new Select(driver.findElement(By.xpath("//div[@id='browseTab']/div[8]/div/select"))).selectByVisibleText("Vopak Terminals Sydney Pty. Ltd. (Site B)");
        new Select(driver.findElement(By.xpath("//div[@id='browseTab']/div[11]/div/select"))).selectByVisibleText("JAN");
        new Select(driver.findElement(By.xpath("//div[@id='browseTab']/div[11]/div[2]/select"))).selectByVisibleText("2022");
        driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Cancel'])[1]/preceding::button[1]")).click();
        driver.get("http://localhost:9000/auctions/secondary");
        if (isElementPresent(By.xpath("/html/body/div[1]/div/div/div/table/tbody")) == true)
        {
            Reporter.log("Successfully added buy listing");
        }
        else
        {
            org.testng.Assert.fail("Adding new buy listing unsuccessful");
        }
        logout();
    }

    @Test (priority = 4)
    public void modifyListing() throws Exception {
        loginAsTrader();
        driver.get("http://localhost:9000/auctions/secondary");
        driver.findElement(By.xpath("//td[10]/span")).click();
        driver.findElement(By.xpath("//div")).click();
        driver.findElement(By.xpath("//input[@type='number']")).clear();
        driver.findElement(By.xpath("//input[@type='number']")).sendKeys("2500");
        driver.findElement(By.xpath("//div[@id='listingTab']/div[6]/div/input")).clear();
        driver.findElement(By.xpath("//div[@id='listingTab']/div[6]/div/input")).sendKeys("350");
        int x = 0;
        while(x <= 24){
            driver.findElement(By.name("price")).sendKeys(Keys.UP);
            x++;
        }
        driver.findElement(By.linkText("Terminal Details")).click();
        driver.findElement(By.xpath("//div[@id='browseTab']/div[2]/label[2]/select")).click();
        new Select(driver.findElement(By.xpath("//div[@id='browseTab']/div[2]/label[2]/select"))).selectByVisibleText("Oceania");
        driver.findElement(By.xpath("//div[@id='browseTab']/div[3]/label[2]/select")).click();
        new Select(driver.findElement(By.xpath("//div[@id='browseTab']/div[3]/label[2]/select"))).selectByVisibleText("Australia");
        driver.findElement(By.xpath("//div[@id='browseTab']/div[4]/label[2]/select")).click();
        new Select(driver.findElement(By.xpath("//div[@id='browseTab']/div[4]/label[2]/select"))).selectByVisibleText("Brisbane");
        driver.findElement(By.xpath("//div[@id='browseTab']/div[6]/div/select")).click();
        new Select(driver.findElement(By.xpath("//div[@id='browseTab']/div[6]/div/select"))).selectByVisibleText("Puma Energy Australia Pty. Ltd. (Brisbane)");
        driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Cancel'])[1]/preceding::button[1]")).click();
        if(driver.getPageSource().contains("Puma Energy Australia Pty. Ltd. (Brisbane)"))
        {
            Reporter.log("Successfully modified buy listing");
        }
        else
        {
            org.testng.Assert.fail("Modifying buy listing unsuccessful");
        }
        logout();
    }

    @Test (priority = 5)
    public void deleteBuyListing()throws Exception{
        deleteListing(true);
        if (isElementPresent(By.xpath("/html/body/div[1]/div/div/div/table/tbody/tr"))) {
            org.testng.Assert.fail("Delting of listing failed");
        } else {
            Reporter.log("Deletion of listing succeeded");
        }
        logout();
    }

    public void deleteListing(Boolean state) throws Exception {
        if(state == true){
            state = false;
            loginAsTrader();
            deleteListing(state);
        }
        driver.get("http://localhost:9000/auctions/secondary");
        if (isElementPresent(By.xpath("/html/body/div[1]/div/div/div/table/tbody[1]"))) {
            driver.findElement(By.xpath("//td[11]/span")).click();
            driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Are you sure you want to cancel this Listing'])[1]/following::button[1]")).click();
            deleteListing(state);
        }
    }

    @Test (priority = 6)
    public void createSellListing() throws Exception {
        loginAsTrader();
        driver.get("http://localhost:9000/auctions/active");
        while (isElementPresent(By.xpath("//div[@id='listingTab']/div[4]/div/form/label[2]/input")) == false ){
            driver.findElement(By.xpath("/html/body/div[1]/nav/div/div[3]/ul[2]/li/button")).click();
        }
        driver.findElement(By.xpath("//div[@id='listingTab']/div[4]/div/form/label[2]/input")).click();
        driver.findElement(By.xpath("//div[@id='listingTab']/div[5]")).click();
        driver.findElement(By.xpath("//input[@type='number']")).clear();
        driver.findElement(By.xpath("//input[@type='number']")).sendKeys("2250");
        driver.findElement(By.xpath("//div[@id='listingTab']/div[6]/div/input")).clear();
        driver.findElement(By.xpath("//div[@id='listingTab']/div[6]/div/input")).sendKeys("50  0");
        int x = 0;
        //Only does it 23 times because the value starts at 0.01 and we're going up to 0.25 for price
        while(x <= 23){
            driver.findElement(By.name("price")).sendKeys(Keys.UP);
            x++;
        }
        driver.findElement(By.linkText("Terminal Details")).click();
        driver.findElement(By.xpath("//div[@id='browseTab']/div[2]/div/form/label[2]/input")).click();
        driver.findElement(By.xpath("//div[@id='browseTab']/div[3]/label[2]/select")).click();
        new Select(driver.findElement(By.xpath("//div[@id='browseTab']/div[3]/label[2]/select"))).selectByVisibleText("Oceania");
        driver.findElement(By.xpath("//div[@id='browseTab']/div[4]/label[2]/select")).click();
        new Select(driver.findElement(By.xpath("//div[@id='browseTab']/div[4]/label[2]/select"))).selectByVisibleText("Australia");
        driver.findElement(By.xpath("//div[@id='browseTab']/div[5]/label[2]/select")).click();
        new Select(driver.findElement(By.xpath("//div[@id='browseTab']/div[5]/label[2]/select"))).selectByVisibleText("Melbourne");
        driver.findElement(By.xpath("//div[@id='browseTab']/div[7]/div/select")).click();
        new Select(driver.findElement(By.xpath("//div[@id='browseTab']/div[7]/div/select"))).selectByVisibleText("Anchor Tank Pty. Ltd. (Melbourne)");
        driver.findElement(By.xpath("//div[@id='browseTab']/div[10]/div/select")).click();
        new Select(driver.findElement(By.xpath("//div[@id='browseTab']/div[10]/div/select"))).selectByVisibleText("JAN");
        driver.findElement(By.xpath("//div[@id='browseTab']/div[10]/div[2]/select")).click();
        new Select(driver.findElement(By.xpath("//div[@id='browseTab']/div[10]/div[2]/select"))).selectByVisibleText("2022");
        driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Cancel'])[1]/preceding::button[1]")).click();
        driver.get("http://localhost:9000/auctions/secondary");
        if (isElementPresent(By.xpath("/html/body/div[1]/div/div/div/table/tbody"))){
            Reporter.log("Create sell of listing succeeded");
        }
        else{
            org.testng.Assert.fail("Create sell listing failed");
        }
        logout();
    }

    @Test (priority = 6)
    public void modifySellListing() throws Exception {
     modifyListing();
    }

    @Test (priority = 7)
    public void deleteSellListing() throws Exception {
        deleteListing(true);
        if (isElementPresent(By.xpath("/html/body/div[1]/div/div/div/table/tbody[1]"))) {
            org.testng.Assert.fail("Delting of listing failed");
        } else {
            Reporter.log("Deletion of listing succeeded");
        }

        logout();
    }

    @Test (priority = 10)
    public void adminEditBrokerInfo(){
        driver.findElement(By.xpath("//button[@type='button']")).click();
        driver.findElement(By.linkText("Admin")).click();
        Reporter.log("Navigating to broker panel");
        driver.findElement(By.linkText("Brokers")).click();
        driver.findElement(By.xpath("//td[9]/span")).click();
        Reporter.log("Entering edited info for broker");
        driver.findElement(By.xpath("//div[3]/div/div/input")).clear();
        driver.findElement(By.xpath("//div[3]/div/div/input")).sendKeys("changedbrokerpass1");
        driver.findElement(By.xpath("//div[4]/div/div/input")).clear();
        driver.findElement(By.xpath("//div[4]/div/div/input")).sendKeys("changedTest");
        driver.findElement(By.xpath("//div[5]/div/div/input")).clear();
        driver.findElement(By.xpath("//div[5]/div/div/input")).sendKeys("changedLastname");
        driver.findElement(By.xpath("//div[6]/div/div/input")).clear();
        driver.findElement(By.xpath("//div[6]/div/div/input")).sendKeys("1234567890");
        driver.findElement(By.xpath("//div[7]/div/div/input")).clear();
        driver.findElement(By.xpath("//div[7]/div/div/input")).sendKeys("changedtestbroker email");
        driver.findElement(By.xpath("//button[@type='button']")).click();
        driver.findElement(By.linkText("Brokers")).click();
        if(driver.getPageSource().contains("changedTest"))
        {
            Reporter.log("Successfully edited broker info");
        }
        else
        {
            org.testng.Assert.fail("Broker failed to be edited");
        }
    }

    @Test (priority = 10)
    public void adminEditBrokerFirmInfo(){
        adminLogin();
        driver.get("http://localhost:9000");
        driver.findElement(By.xpath("//button[@type='button']")).click();
        driver.findElement(By.linkText("Admin")).click();
        Reporter.log("Navigating to broker firm panel");
        driver.findElement(By.linkText("Broker Firms")).click();
        driver.findElement(By.xpath("//td[5]/span")).click();
        Reporter.log("Entering editing info for trading firm");
        driver.findElement(By.xpath("//input[@type='text']")).clear();
        driver.findElement(By.xpath("//input[@type='text']")).sendKeys("ChangedTestBrokerFirm");
        driver.findElement(By.xpath("//textarea[@type='text']")).clear();
        driver.findElement(By.xpath("//textarea[@type='text']")).sendKeys("24 New Broker firm street");
        driver.findElement(By.xpath("//div[4]/div/div/input")).clear();
        driver.findElement(By.xpath("//div[4]/div/div/input")).sendKeys("Brokerton");
        driver.findElement(By.xpath("//div[5]/div/div/input")).clear();
        driver.findElement(By.xpath("//div[5]/div/div/input")).sendKeys("New State");
        driver.findElement(By.xpath("//div[6]/div/div/input")).clear();
        driver.findElement(By.xpath("//div[6]/div/div/input")).sendKeys("17645240");
        driver.findElement(By.xpath("//div[7]/div/div/input")).clear();
        driver.findElement(By.xpath("//div[7]/div/div/input")).sendKeys("Changed Country");
        driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Trading Firms'])[1]/following::div[2]")).click();
        driver.findElement(By.xpath("//div/div/div/div/input")).click();
        driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Country'])[1]/following::div[3]")).click();
        driver.findElement(By.xpath("//button[@type='button']")).click();
        driver.findElement(By.linkText("Broker Firms")).click();
        if(driver.getPageSource().contains("ChangedTestBrokerFirm"))
        {
            Reporter.log("Successfully edited broker firm info");
        }
        else
        {
            org.testng.Assert.fail("Broker firm failed to be edited");
        }
    }

    @Test (priority = 10)
    public void adminEditTraderInfo(){
        driver.get("http://localhost:9000");
        driver.findElement(By.xpath("//button[@type='button']")).click();
        driver.findElement(By.linkText("Admin")).click();
        Reporter.log("Navigating to broker firm panel");
        driver.findElement(By.linkText("Traders")).click();
        driver.findElement(By.xpath("//td[10]/span")).click();
        Reporter.log("Entering editing info for trader");
        driver.findElement(By.xpath("//div[3]/div/div/input")).click();
        driver.findElement(By.xpath("//div[3]/div/div/input")).sendKeys("changedpassword");
        driver.findElement(By.xpath("(.//*[normalize-space(text()) and normalize-space(.)='Password'])[1]/following::div[3]")).click();
        driver.findElement(By.xpath("//div[4]/div/div/input")).clear();
        driver.findElement(By.xpath("//div[4]/div/div/input")).sendKeys("Billy");
        driver.findElement(By.xpath("//div[5]/div/div/input")).clear();
        driver.findElement(By.xpath("//div[5]/div/div/input")).sendKeys("Mays");
        driver.findElement(By.xpath("//div[6]/div/div/input")).clear();
        driver.findElement(By.xpath("//div[6]/div/div/input")).sendKeys("8888855555");
        driver.findElement(By.xpath("//div[7]/div/div/input")).clear();
        driver.findElement(By.xpath("//div[7]/div/div/input")).sendKeys("newemail");
        driver.findElement(By.xpath("//button[@type='button']")).click();
        driver.findElement(By.linkText("Traders")).click();
        if(driver.getPageSource().contains("Billy"))
        {
            Reporter.log("Successfully edited trader details");
        }
        else
        {
            org.testng.Assert.fail("Trader editing unsuccessful");
        }
        removeAccount();
    }

    public void removeAccount(){
        driver.get("http://localhost:9000/users/traders");
        driver.findElement(By.linkText("Accounts")).click();
        driver.findElement(By.xpath("//*/text()[normalize-space(.)='traderacc1']/parent::*")).click();
        driver.findElement(By.xpath("//span")).click();
        driver.findElement(By.xpath("//button[@type='button']")).click();
        Reporter.log("Account successfully removed");
    }

    @Test (priority = 10)
    public void adminEditTradingFirmInfo(){
        driver.get("http://localhost:9000/auctions/active");
        driver.findElement(By.xpath("//button[@type='button']")).click();
        driver.findElement(By.linkText("Admin")).click();
        Reporter.log("Navigating to broker firm panel");
        driver.findElement(By.linkText("Trading Firms")).click();
        driver.findElement(By.xpath("//tr/td[7]/span")).click();
        Reporter.log("Entering editing info for trading firm");
        driver.findElement(By.xpath("//textarea[@type='text']")).clear();
        driver.findElement(By.xpath("//textarea[@type='text']")).sendKeys("Changed TestAddress 123");
        driver.findElement(By.xpath("//div[4]/div/div/input")).clear();
        driver.findElement(By.xpath("//div[4]/div/div/input")).sendKeys("ChangedTestCity");
        driver.findElement(By.xpath("//div[5]/div/div/input")).clear();
        driver.findElement(By.xpath("//div[5]/div/div/input")).sendKeys("ChangedTestState");
        driver.findElement(By.xpath("//div[6]/div/div/input")).clear();
        driver.findElement(By.xpath("//div[6]/div/div/input")).sendKeys("12345678");
        driver.findElement(By.xpath("//div[7]/div/div/input")).clear();
        driver.findElement(By.xpath("//div[7]/div/div/input")).sendKeys("NewTestCountry");
        driver.findElement(By.xpath("//button[@type='button']")).click();
        driver.findElement(By.linkText("Trading Firms")).click();
        if(driver.getPageSource().contains("Changed TestAddress 123"))
        {
            Reporter.log("Successfully edited trading firm details");
        }
        else
        {
            org.testng.Assert.fail("Trading firm editing unsuccessful");
        }
    }

    private void adminLogin(){
        driver.get("http://localhost:9000/login");
        driver.findElement(By.xpath("//input[@name='username']")).clear();
        driver.findElement(By.xpath("//input[@name='username']")).sendKeys("testadmin");
        driver.findElement(By.xpath("//input[@name='password']")).clear();
        driver.findElement(By.xpath("//input[@name='password']")).sendKeys("testadmin");
        driver.findElement(By.id("submitButton")).click();
    }

    private void logout(){
        driver.findElement(By.linkText("Log Out")).click();
    }

    private boolean isElementPresent(By by) {
        try {
            driver.findElement(by);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    @AfterClass(alwaysRun = true)
    public void tearDown(){
        logout();
        driver.quit();
        String verificationErrorString = verificationErrors.toString();
        if (!"".equals(verificationErrorString)) {
            fail(verificationErrorString);
        }
    }

}