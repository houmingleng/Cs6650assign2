package Servlet.part2;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import io.swagger.client.model.LiftRide;
import module.ChannelPool;

import java.io.BufferedReader;
import java.io.PrintWriter;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class SkierServlet extends HttpServlet {
    private Gson gson;
    public static class Message{
        String message;
        public Message(String msg) {
            message = msg;
        }
    }
    private Message outputMsg = new Message("hello");
    private  String resortId;
    private  String skierId;
    private  String dayId;
    private  String seasonId;
    private ChannelPool channelPool;
    private  final  static String QUEUE_NAME= "hello";

    private enum HttpRequestStatus{
        GET_NO_PARAM,
        GET_SKIERS_WITH_RESORT_SEASON_DAY_ID,
        POST_SKIERS_WITH_RESORT_SEASON_DAY_ID,
        GET_VERTICAL_WITH_ID,
        POST_SEASONS_WITH_RESORT,
        NOT_VALID
    }

    @Override
    public void init() throws  ServletException{
        try {
            super.init();
            System.out.println("begin");
            channelPool = new ChannelPool();
        } catch (IOException | TimeoutException e) {
           // throw new RuntimeException(e);
            e.printStackTrace();
        }
    }
    private void handleWithoutParam(HttpServletResponse res) throws IOException{
        res.setContentType("text/plain");
        res.setStatus(HttpServletResponse.SC_OK);
        try{
            PrintWriter out = res.getWriter();
            res.setContentType("application/json");
            res.setCharacterEncoding("UTF-8");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain");
        String urlPath = request.getPathInfo();
        HttpRequestStatus curStatus = checkStatus(urlPath, "POST");
        PrintWriter out = response.getWriter();
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        if(!curStatus.equals(HttpRequestStatus.NOT_VALID)) {
            response.setStatus(HttpServletResponse.SC_OK);
            if(curStatus.equals(HttpRequestStatus.GET_NO_PARAM)) handleWithoutParam(response);
            else{
                out.write(gson.toJson(outputMsg));
                out.flush();
            }
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.write(gson.toJson(outputMsg));
            out.flush();
        }
    }
    private HttpRequestStatus checkStatus(String urlPath, String type) {
        if(urlPath == null || urlPath.isEmpty()) return HttpRequestStatus.GET_NO_PARAM;
        String resortID = "";
        String seasons = "";
        String dayID = "";
        String skierID = "";
        String[] urlParts = urlPath.split("/");
        if(urlParts.length == 8) {
            if(!urlParts[2].equals("seasons") || !urlParts[4].equals("days") || !urlParts[6].equals("skiers")) {
                outputMsg = new Message("Page2 Not Found");
                return HttpRequestStatus.NOT_VALID;
            }

            if(!isValidNumber(resortID) || !isValidNumber(dayID)
                    || !isValidNumber(skierID)) {
                outputMsg = new Message("Invalid Input Information");
                return HttpRequestStatus.NOT_VALID;
            }
            this.resortId = resortID;
            this.seasonId = seasons;
            this.dayId = dayID;
            this.skierId = skierID;
            if(type.equals("GET"))
                return HttpRequestStatus.GET_SKIERS_WITH_RESORT_SEASON_DAY_ID;
            else return HttpRequestStatus.POST_SKIERS_WITH_RESORT_SEASON_DAY_ID;

        } else if(urlParts.length == 3) {
            if(!urlParts[2].equals("vertical")) {
                outputMsg = new Message("Page3 Not Found");
                return HttpRequestStatus.NOT_VALID;
            }
            resortID = urlParts[1];
            if(!isValidNumber(resortID)) {
                outputMsg = new Message("Invalid resortNumber");
                return HttpRequestStatus.NOT_VALID;
            }
            return HttpRequestStatus.GET_VERTICAL_WITH_ID;
        } else {
            outputMsg = new Message(String.valueOf(urlParts.length));
            return HttpRequestStatus.NOT_VALID;
        }
    }
    private boolean isValidNumber(String s) {
        if (s == null || s.isEmpty()) return false;
        try {
            int digits = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }
    private LiftRide getReqBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        BufferedReader reader = req.getReader();
        try{
            while((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        LiftRide liftRide = gson.fromJson(sb.toString(), LiftRide.class);
        return liftRide;
    }
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain");
        String urlPath = request.getPathInfo();
        HttpRequestStatus curStatus = checkStatus(urlPath, request.getMethod());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        if(!curStatus.equals(HttpRequestStatus.NOT_VALID)) {
            response.setStatus(HttpServletResponse.SC_OK);
            if(curStatus.equals(HttpRequestStatus.GET_NO_PARAM)) handleWithoutParam(response);
            else{
                LiftRide liftRide = getReqBody(request);

                liftRide.setLiftID(Integer.parseInt(this.skierId));
                liftRide.setTime(Integer.parseInt(this.dayId));

//                String msg = "SkierID: " + skierID + "has successfully uploaded" +
//                        "liftRide information #" + liftRide.getLiftID() + "@" + seasonID + "_" +
//                        dayID + "_" + resortID;
                String message = gson.toJson(liftRide);
                if(sendMessageToQueue(message)) {
                    response.getWriter().write(gson.toJson(new Message("a")));
                } else {
                    response.getWriter().write("not success");
                }
                response.getWriter().flush();
            }
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.write(gson.toJson(outputMsg));
            out.flush();
        }
    }
    private boolean sendMessageToQueue(String msg) {
        try {
            Channel channel = channelPool.getChannel();
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            channel.basicPublish("", QUEUE_NAME,
                    null,msg.getBytes(StandardCharsets.UTF_8));
            channelPool.add(channel);
            return true;
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
