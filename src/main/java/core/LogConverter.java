package core;

import helper.Utils;
import models.*;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Pattern;

public class LogConverter {

    /**
     * converts a list of logs in String format to a list of operations for publishing
     * logs a handled differently based on the type of operation
     * @param logs the logs to convert
     * @return the list of operations
     */
    ArrayList<Operation> convertLogs(ArrayList<String> logs) {
        ArrayList<Operation> operations = new ArrayList<>();
        for (String log : logs) {
            String statement = log.split(Pattern.quote("jdbc.sqlonly - "))[1];
            Date date = Utils.getDateFromLogString(log.split(Pattern.quote(" ["))[0]);
            String[] parts = statement.split(" ");
            long time = 0;
            if (date != null) {
                time = date.getTime();
            }
            Operation operation;
            switch (parts[0]) {
                case "INSERT":
                    operation = buildInsert(statement, time);
                    if (operation != null) operations.add(operation);
                    break;
                case "UPDATE":
                    operation = buildUpdate(statement, time);
                    if (operation != null) operations.add(operation);
                    break;
                case "DELETE":
                    operation = buildDelete(statement, time);
                    if (operation != null) operations.add(operation);
                    break;
            }
        }
        return operations;
    }

    /**
     * builds an insert operation
     * @param statement the log containing the insert statement
     * @param time the time the conversion took place
     * @return returns an insert operation
     */
    private Operation buildInsert(String statement, long time) {
        String[] parts = statement.split(" ");
        OperationType operationType = OperationType.valueOf(parts[0]);
        ObjectType model = ObjectType.valueOf(parts[2].split(Pattern.quote("("))[0]);
        Object object = null;
        String parameters = StringUtils.substringBetween(statement, "VALUES(", ")");
        switch (model) {
            case BOOK:
                object = buildBook(parameters);
                break;
            case MEMBER:
                object = buildMember(parameters);
                break;
            case ISSUE:
                object = buildIssue(parameters, time);
                break;
            case MAIL_SERVER_INFO:
                object = buildMailServerInfo(parameters);
                break;
            default: break;
        }
        if (object != null) {
            return new Operation(time, operationType, model, object);
        }
        return null;
    }

    /**
     * builds an update operation
     * @param statement the log containing the update statement
     * @param time the time the conversion took place
     * @return returns an update operation
     */
    private Operation buildUpdate(String statement, long time) {
        String[] parts = statement.split(" ");
        ObjectType model = ObjectType.valueOf(parts[1]);
        Object object = null;
        switch (model) {
            case BOOK: {
                String id = StringUtils.substringBetween(statement, "ID='", "'");
                String title = StringUtils.substringBetween(statement, "TITLE='", "'");
                String author = StringUtils.substringBetween(statement, "AUTHOR='", "'");
                String publisher = StringUtils.substringBetween(statement, "PUBLISHER='", "'");
                String isAvailable = StringUtils.substringBetween(statement, "isAvail=", " W");
                Boolean isAvailableBool = null;
                if (isAvailable != null) {
                    isAvailableBool = isAvailable.equals("true");
                }
                object = new Book(id, title, author, publisher, isAvailableBool);
                break;
            }
            case MEMBER: {
                String id = StringUtils.substringBetween(statement, "ID='", "'");
                String name = StringUtils.substringBetween(statement, "NAME='", "'");
                String email = StringUtils.substringBetween(statement, "EMAIL='", "'");
                String mobile = StringUtils.substringBetween(statement, "MOBILE='", "'");
                object = new Member(id, name, mobile, email);
                break;
            }
            case ISSUE: {
                String bookId = StringUtils.substringBetween(statement, "BOOKID='", "'");
                String renewCount = StringUtils.substringBetween(statement, "renew_count=", " W");
                object = new Issue(bookId, renewCount, time);
                break;
            }
            case MAIL_SERVER_INFO: {
                // mailServerInfo is never updated
                // System.out.println("mailServerInfo");
                break;
            }
            default: break;
        }
        if (object != null) {
            return new Operation(time, OperationType.UPDATE, model, object);
        }
        return null;
    }

    /**
     * builds an delete operation
     * @param statement the log containing the delete statement
     * @param time the time the conversion took place
     * @return returns an delete operation
     */
    private Operation buildDelete(String statement, long time) {
        String[] parts = statement.split(" ");
        ObjectType model = ObjectType.valueOf(parts[2]);
        Object object = null;
        switch (model) {
            case BOOK: {
                String id = StringUtils.substringBetween(statement, "ID='", "'");
                String title = StringUtils.substringBetween(statement, "TITLE='", "'");
                String author = StringUtils.substringBetween(statement, "AUTHOR='", "'");
                String publisher = StringUtils.substringBetween(statement, "PUBLISHER='", "'");
                object = new Book(id, title, author, publisher, null);
                break;
            }
            case MEMBER: {
                String id = StringUtils.substringBetween(statement, "ID='", "'");
                object = new Member(id);
                break;
            }
            case ISSUE: {
                String bookId = StringUtils.substringBetween(statement, "BOOKID='", "'");
                object = new Issue(bookId);
                break;
            }
            case MAIL_SERVER_INFO: {
                object = new MailServerInfo();
                break;
            }
            default: break;
        }
        if (object != null) {
            return new Operation(time, OperationType.DELETE, model, object);
        }
        return null;
    }

    /**
     * builds a book object from parameters in string format
     * @param parameters the parameters from which the book is built
     * @return the newly built book
     */
    private Book buildBook(String parameters) {
        String[] parts = removeQuotes(parameters.split(","));
        return new Book(parts[0], parts[1], parts[2], parts[3], parts[3].equals("1"));
    }

    /**
     * builds a member object from parameters in string format
     * @param parameters the parameters from which the member is built
     * @return the newly built member
     */
    private Member buildMember(String parameters) {
        String[] parts = removeQuotes(parameters.split(","));
        return new Member(parts[0], parts[1], parts[2], parts[3]);
    }

    /**
     * builds an issue object from parameters in string format
     * @param parameters the parameters from which the issue is built
     * @param time the time the issue was given
     * @return the newly built issue
     */
    private Issue buildIssue(String parameters, long time) {
        String[] parts = removeQuotes(parameters.split(","));
        return new Issue(parts[0], parts[1], "0", time);
    }

    /**
     * builds a mailServerInfo object from parameters in string format
     * @param parameters the parameters from which the mailServerInfo is built
     * @return the newly built mailServerInfo
     */
    private MailServerInfo buildMailServerInfo(String parameters) {
        String[] parts = removeQuotes(parameters.split(","));
        return new MailServerInfo(parts[0], Integer.valueOf(parts[1]), parts[2], parts[3], parts[4].equals("1"));
    }

    /**
     * removes quotes from string parameters
     * example: 'test' -> test
     * @param parameters string containing quotes
     * @return the parameters with the quotes removed
     */
    private String[] removeQuotes(String[] parameters) {
        for (int i = 0; i < parameters.length; i++) {
            parameters[i] = parameters[i].replace("'", "");
        }
        return parameters;
    }

}
