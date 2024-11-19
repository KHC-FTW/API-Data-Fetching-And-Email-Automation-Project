package com.crc2jasper.jiraK2DataFetching;

import java.util.Map;

public class EmailHTML {
    public static String emailHTMLDom(int relatedCnt, String related, int unrelatedCnt, String unrelated){
        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
                    <style>
                        table {
                            width: 100%%;
                        }
                
                        table,
                        th,
                        td {
                            border: 1px solid black;
                            border-collapse: collapse;
                        }
                
                        th,
                        td {
                            padding: 15px;
                            text-align: left;
                        }
                
                        table#t01 th {
                            background-color: yellow;
                            color: black;
                        }
                
                        table#t02 th {
                            background-color: gray;
                            color: lightgrey;
                        }
                    </style>
                </head>
                <body>
                    <h1 style="color: red; text-decoration: underline;">IMP Promotions (%d):</h1>
                    <table id="t01">
                        <tr>
                            <th max-width="110">Target Date</th>
                            <th width="130">Summary</th>
                            <th max-width="300">Description</th>
                            <th width="155">Promotion Form</th>
                            <th width="130">Type(s)</th>
                            <th max-width="100">Status</th>
                        </tr>
                        %s
                    </table>
                    <br>
                    <hr>
                    <h1 style="color: gray;">Unrelated (%d):</h1>
                    <table id="t02">
                        <tr>
                            <th max-width="110">Target Date</th>
                            <th width="130">Summary</th>
                            <th max-width="300">Description</th>
                            <th width="155">Promotion Form</th>
                            <th width="130">Type(s)</th>
                            <th max-width="100">Status</th>
                        </tr>
                        %s
                    </table>
                    <br>
                </body>
                </html>
                """, relatedCnt, related, unrelatedCnt, unrelated);
    }

    public static String genTableContent(EmailForm emailForm){
        String highlightStyle = " style=\"background-color: lightgreen; font-weight: bold;\"";
        String addStyle = emailForm.isToday() ? highlightStyle : "";
        return String.format("""
                <tr%s>
                  	 <td>%s</td>
                  	 <td><a href="https://hatool.home/jira/browse/%s" target="_blank">%s</a></td>
                  	 <td>%s</td>
                  	 <td><a href="%s" target="_blank">%s</td>
                  	 <td>%s</td>
                  	 <td>%s</td>
                </tr>
                """,
                addStyle,
                dateHighlight(emailForm.getTargetDate()),
                emailForm.getKey(),
                emailForm.getSummary(),
                replaceWithHTMLbrTag(emailForm.getDescription()),
                emailForm.getPromotionFormLink(),
                emailForm.getPromotionFormNo(),
                formatType(emailForm.getTypes()),
                emailForm.getStatus()
                );
    }

    private static String dateHighlight(String date){
        if (date.contains("Sat") || date.contains("Sun")){
            return String.format("<span style=\"color: red; font-weight: bold;\">%s</span>", date);
        }
        return date;
    }

    private static String replaceWithHTMLbrTag(String input){
        return input.replaceAll("\n", "<br>");
    }

    private static String formatType(Map<Integer, String> emailFormTypes){
        StringBuilder results = new StringBuilder();
        for (String type: emailFormTypes.values()){
            results.append(type).append("<br>");
        }
        return results.toString();
    }
}
