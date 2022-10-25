package Servlet.part1;

import com.google.gson.Gson;
import com.rabbitmq.client.*;
import io.swagger.client.model.LiftRide;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

//@WebServlet(name = "ServletStep1", value = "/ServletStep1")
public class ServletStep1 extends HttpServlet implements AutoCloseable  {

    private Gson gson = new Gson();
    private Connection connection;
    private Channel channel;
    private String requestQueueName = "rpc_queue";
    public static class Message{
        String message;
        public Message(String msg) {
            message = msg;
        }
    }
    private Message outputMsg = new Message("hello");
    ConnectionFactory factory ;

    @Override
    public void  init() {
        try {
            System.out.println("start");
            super.init();
            this.factory = new ConnectionFactory();
            try {
                connection = this.factory.newConnection();
            } catch (TimeoutException | IOException e) {
                throw new RuntimeException(e);
            }
            channel = connection.createChannel();
//            channelPool = new ChannelPool();
        } catch (ServletException | IOException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/plain");
        String urlPath = req.getPathInfo();
        PrintWriter out = res.getWriter();
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        // check we have a URL!
        if (urlPath == null || urlPath.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write("missing paramterers");
            return;
        }
        System.out.println(urlPath);
      //  LiftRide liftRide = getReqBody(req);
       // System.out.println(liftRide.toString());
        String[] urlParts = urlPath.split("/");
        // and now validate url path and return the response status code
        // (and maybe also some value if input is valid)

        if (!isUrlValid(urlPath)) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            out.write(gson.toJson(new Message(urlPath)));
            res.setStatus(HttpServletResponse.SC_OK);
            // do any sophisticated processing with urlParts which contains all the url params
            // TODO: process url params in `urlParts`
            res.getWriter().write("It works!");
        }
    }
    private boolean isUrlValid(String urlPath) {
        // TODO: validate the request url path according to the API spec
        // urlPath  = "/1/seasons/2019/day/1/skier/123"
        // urlParts = [, 1, seasons, 2019, day, 1, skier, 123]
        if(urlPath == null || urlPath.isEmpty()) return false;
        return true;
    }
    public void close() throws IOException {
        connection.close();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("text/plain");
        String urlPath = req.getPathInfo();
        PrintWriter out = res.getWriter();
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        // check we have a URL!
        if (urlPath == null || urlPath.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write("missing paramterers");
            return;
        }
        System.out.println(urlPath);
        StringBuilder sb = new StringBuilder();
        String line;
        BufferedReader reader = req.getReader();

        try{
            while((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            System.out.println("wrong");
            e.printStackTrace();
        }
       // LiftRide liftRide = gson.fromJson(sb.toString(), LiftRide.class);
        System.out.println(sb.toString());
        String[] urlParts = urlPath.split("/");
        // and now validate url path and return the response status code
        // (and maybe also some value if input is valid)
            // here begin the redditmq
        factory.setHost("localhost");


        String response="";
        try {
             response = call(sb.toString() + urlPath);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        System.out.println(response);


        if (!isUrlValid(urlPath)) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            out.write(gson.toJson(outputMsg));
            res.setStatus(HttpServletResponse.SC_OK);
            // do any sophisticated processing with urlParts which contains all the url params
            // TODO: process url params in `urlParts`
            res.getWriter().write("It works!");
        }
    }
    public String call(String message) throws IOException, InterruptedException, ExecutionException {
        final String corrId = UUID.randomUUID().toString();

        String replyQueueName = channel.queueDeclare().getQueue();
        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .correlationId(corrId)
                .replyTo(replyQueueName)
                .build();

        channel.basicPublish("", requestQueueName, props, message.getBytes("UTF-8"));

        final CompletableFuture<String> response = new CompletableFuture<>();

        String ctag = channel.basicConsume(replyQueueName, true, (consumerTag, delivery) -> {
            if (delivery.getProperties().getCorrelationId().equals(corrId)) {
                response.complete(new String(delivery.getBody(), "UTF-8"));
            }
        }, consumerTag -> {
        });

        String result = response.get();
        channel.basicCancel(ctag);
        return result;
    }
}
