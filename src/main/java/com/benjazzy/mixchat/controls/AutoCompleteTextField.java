package com.benjazzy.mixchat.controls;

import com.benjazzy.mixchat.MixChat;
import com.benjazzy.mixchat.commands.Commands;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class is a TextField which implements an "autocomplete" functionality, based on a supplied list of entries.
 * @author Caleb Brinkman
 */
public class AutoCompleteTextField extends TextField
{
    /** The existing commands autocomplete entries. */
    private final SortedSet<String> commandEntries;
    /** The popup used to select a command entry. */
    private ContextMenu commandsPopup;
    /** The existing username autocomplete entries. */
    private final SortedSet<String> usernameEntries;
    /** The popup used to select a username entry. */
    private ContextMenu usernamesPopup;

    /** Commands object that stores a list the commands */
    private Commands commands = new Commands();

    /** MixChat object used to get a list of users */
    private MixChat chat;

    /** Construct a new AutoCompleteTextField. */
    public AutoCompleteTextField() {
        super();
        commandEntries = new TreeSet<>();
        commandEntries.addAll(commands.getCommands());
        commandsPopup = new ContextMenu();

        usernameEntries = new TreeSet<>();
        usernamesPopup = new ContextMenu();

        // Add a listener to display both the commandsPopup and usernamesPopup.
        textProperty().addListener((observableValue, oldValue, newValue) -> {
            // Check if there is no text in the TextField or if the current text is
            // equal to a command and if so the close all popups.
            if (getText().length() == 0 || getText().equals(commands.get(getText())))
            {
                commandsPopup.hide();
                usernamesPopup.hide();

            }
            else
            {
                // Split up the current text into its words.
                String[] messageComponents = getText().split(" ");

                // If a character has been deleted subtract two from the caret position.
                int position = getCaretPosition() - (oldValue.length() > newValue.length() ? 2 : 0);

                // Get the current word by searching backwards for either a space or an @ symbol.
                String currentWord = "";
                for (int i = position; i >= 0; i--) {
                    if (newValue.charAt(i) == ' ' || newValue.charAt(i) == '@') {
                        currentWord = newValue.substring(i, position + 1);
                        break;
                    }
                }

                // Check if the text is a command. If so open the commandPopup.
                if (getText().startsWith("/")) {
                    LinkedList<String> commandsList = new LinkedList<>();
                    commandsList.addAll(commandEntries.subSet(getText(), getText() + Character.MAX_VALUE));
                    if (commandEntries.size() > 0) {
                        populateCommandsPopup(commandsList);
                        if (!commandsPopup.isShowing()) {
                            commandsPopup.show(AutoCompleteTextField.this, Side.BOTTOM, 0, 0);
                        }
                    }
                }
                else
                    commandsPopup.hide();
                // Check to see the current word is a username and if so open the usernamesPopup.
                if (currentWord.startsWith("@")) {
                    if (messageComponents.length != 0 && chat != null) {
                        if (currentWord.startsWith("@")) {
                            usernameEntries.addAll(chat.getUsers().stream().map(o -> "@" + o.getUserName()).collect(Collectors.toList()));
                            LinkedList<String> usernameList = new LinkedList<>();
                            usernameList.addAll(usernameEntries.subSet(currentWord, currentWord + Character.MAX_VALUE));
                            if (chat != null) {
                                populateUsernamePopup(usernameList);
                                if (!usernamesPopup.isShowing()) {
                                    usernamesPopup.show(AutoCompleteTextField.this, Side.BOTTOM, 0, 0);
                                }
                            }
                        }
                    }
                }
                else
                    usernamesPopup.hide();
            }
        });

        focusedProperty().addListener((observableValue, aBoolean, aBoolean2) -> commandsPopup.hide());

        // Handle tab presses to autocomplete.
        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            // Check if the pressed key is tab.
            if (event.getCode() == KeyCode.TAB) {
                // Split the current text into individual words.
                String[] messageComponents = getText().split(" ");
                // Add tab autocompletion for commands.
                if (commands.isCommand(getText()) && commands.getCommands(getText()).size() == 1) {
                    autocompleteCommand();
                }
                // Check if there is more that one word in the current text.
                else if (messageComponents.length != 0) {
                    // Get the current word by searching backwards for either a space or an @ symbol.
                    String currentWord = "";
                    for (int i = getCaretPosition() - 1; i >= 0; i--) {
                        if (getText().charAt(i) == ' ' || getText().charAt(i) == '@') {
                            currentWord = getText().substring(i, getCaretPosition());
                            break;
                        }
                    }
                    // Checks if the current word that starts with an @.
                    if (currentWord.startsWith("@")) {
                        LinkedList<String> usernameList = new LinkedList<>();
                        usernameList.addAll(usernameEntries.subSet(currentWord, currentWord + Character.MAX_VALUE));
                        // Checks if there is only one entry in usernameList.
                        if (usernameList.size() == 1) {
                            autocompleteUsername(usernameList.get(0));
                        }
                    }
                }
                // Consume the event so focus doesn't change.
                event.consume();
            }
        });
    }

    /**
     * Populate the command entry set with the given search results.  Display is limited to 10 entries, for performance.
     * @param commandsResult The set of matching strings.
     */
    private void populateCommandsPopup(List<String> commandsResult) {
        List<CustomMenuItem> menuItems = new LinkedList<>();
        // If you'd like more entries, modify this line.
        int maxEntries = 10;
        int count = Math.min(commandsResult.size(), maxEntries);
        for (int i = 0; i < count; i++)
        {
            final String result = commandsResult.get(i);
            Label entryLabel = new Label(result);
            CustomMenuItem item = new CustomMenuItem(entryLabel, true);
            item.setOnAction(actionEvent -> {
                autocompleteCommand();
            });
            menuItems.add(item);
        }
        commandsPopup.getItems().clear();
        commandsPopup.getItems().addAll(menuItems);
    }

    /**
     * Populate the username entry set with the given search results.  Display is limited to 10 entries, for performance.
     * @param usernameResults The set of matching strings.
     */
    private void populateUsernamePopup(List<String> usernameResults) {
        List<CustomMenuItem> menuItems = new LinkedList<>();
        // If you'd like more entries, modify this line.
        int maxEntries = 10;
        int count = Math.min(usernameResults.size(), maxEntries);
        for (int i = 0; i < count; i++)
        {
            final String result = usernameResults.get(i);
            Label entryLabel = new Label(result);
            CustomMenuItem item = new CustomMenuItem(entryLabel, true);
            // On action update the text property with the corresponding username.
            item.setOnAction(actionEvent -> {
                autocompleteUsername(result);
            });
            menuItems.add(item);
        }
        usernamesPopup.getItems().clear();
        usernamesPopup.getItems().addAll(menuItems);
    }

    /**
     * Autocompletes the currently entered command.
     */
    private void autocompleteCommand() {
        setText(commands.get(getText()) + " ");
        positionCaret(getText().length());
        commandsPopup.hide();
    }

    /**
     * Autocompletes the username at the caret position.
     *
     * @param username Username to autocomplete.
     */
    private void autocompleteUsername(String username) {
        // Logic to check if a space needs to be added after the autocomplete or if a space is already there.
        String space = " ";
        if (getText().length() > getCaretPosition()) {
            if (getText().charAt(getCaretPosition()) == ' ') {
                space = "";
            }
        }
        // Get the current word by searching backwards for either a space or an @ symbol.
        String currentWord = "";
        for (int j = getCaretPosition() - 1; j >= 0; j--) {
            if (getText().charAt(j) == ' ' || getText().charAt(j) == '@') {
                currentWord = getText().substring(j, getCaretPosition());
                break;
            }
        }

        // Insert addText into the current text.
        String addText = username.substring(currentWord.length()) + space;
        int position = getCaretPosition() + addText.length();
        StringBuffer textBuffer = new StringBuffer(getText());
        textBuffer.insert(getCaretPosition(), addText);
        setText(textBuffer.toString());
        positionCaret(position);
        usernamesPopup.hide();
    }

    public void setMixChat(MixChat chat) {
        this.chat = chat;
    }
}