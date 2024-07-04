The files in this GitHub contain sample code from my previous work, with AccountTests being an end to end test of user account functionality, and the other 4 files being examples of my front and back end development using the Play Framework.

TerminalController: The terminal controller class contains all terminal-related functions that will be called by a client. Play handles this through API calls from the client to the server, which will be present in the terminal-dialog.js. In this class, JSON objects are received by the server, the values of which are then extracted and used to call functions in the TerminalProcessor class

TerminalProcessor: Handles functions for creating, updating and removing terminals, as well as several functions for retrieving terminal stats.

terminal-dialogs: JavaScript for client functionality. Handles creating or updating terminals via an API call to the server.

terminalForm: HTML for the terminal dialog, where users can add or update terminals. This is manipulated by the terminal-dialogs file via angularJS


