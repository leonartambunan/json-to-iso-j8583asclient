//package id.co.nio;
//
//import com.github.kpavlov.jreactive8583.IsoMessageListener;
//import com.github.kpavlov.jreactive8583.server.Iso8583Server;
//import com.solab.iso8583.IsoMessage;
//import com.solab.iso8583.IsoType;
//import com.solab.iso8583.IsoValue;
//import io.netty.channel.ChannelHandlerContext;
//import org.jetbrains.annotations.NotNull;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import redis.clients.jedis.Jedis;
//import redis.clients.jedis.JedisPool;
//import redis.clients.jedis.JedisPubSub;
//
//import javax.annotation.PostConstruct;
//import javax.annotation.PreDestroy;
//import java.nio.charset.StandardCharsets;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.concurrent.*;
//
//@Component
////public class RedisPubSub implements ServletContextListener, IsoMessageListener<IsoMessage> {
//public class RedisPubSub implements IsoMessageListener<IsoMessage> {
//    @Autowired
//    JedisPool jedisPool;
//
//    @Autowired
//    Iso8583Server iso8583Server;
//
//    public static final String REDIS_CHANNEL_REQUEST  = "channel_iso_online_req";
//    public static final String REDIS_CHANNEL_RESPONSE = "channel_iso_online_res";
//
////    @Override
////    public void contextInitialized(ServletContextEvent sce) {
////        init();
////    }
//
////    @Override
////    public void contextDestroyed(ServletContextEvent sce) {
////        destroy();
////    }
//
//    private final JedisPubSub jedisPubSub = new JedisPubSub() {
//        @Override
//        public void onMessage(String channel, String incomingMessage) {
//            logger.info("onMessage");
//            if (REDIS_CHANNEL_REQUEST.equals(channel)) {
//                logger.info(incomingMessage);
//
//                try {
//
//                    final IsoMessage m = (IsoMessage) iso8583Server.getIsoMessageFactory().parseMessage(incomingMessage.getBytes(), 0);
//
//                    String f7 = m.getField(7).toString();
//                    String f11 = m.getField(11).toString();
//                    String identifier = f7+f11;
//
//                    final CountDownLatch latch = new CountDownLatch(1);
//                    latchMap.put(identifier,latch);
//
//                    IsoMessage response;
//
//                    if (ISOServer.channelHandlerContext==null
//                            || ISOServer.channelHandlerContext.channel() == null
//                        || !ISOServer.channelHandlerContext.channel().isOpen()) {
//                        logger.error("bodoh juga");
//                        response = (IsoMessage) iso8583Server.getIsoMessageFactory().createResponse(m);
//                        response.setField(7, new IsoValue<>(IsoType.DATE10,f7));
//                        response.setField(11,new IsoValue<>(IsoType.NUMERIC,f11,6));
//                        response.setField(39,new IsoValue<>(IsoType.ALPHA,"06",2));
//                    } else {
//
//                        logger.info("Sending to the counter-part");
//                        ISOServer.channelHandlerContext.writeAndFlush(m);
//
//                        latch.await(7, TimeUnit.SECONDS);
//
//                        logger.warn("getting " + identifier);
//                        response = futureResult.get(identifier);
//
//                        futureResult.remove(identifier);
//                        latchMap.remove(identifier);
//
//                        if (response == null) {
//                            logger.error("bodoh");
//                            response = (IsoMessage) iso8583Server.getIsoMessageFactory().createResponse(m);
//                            response.setField(7, new IsoValue<>(IsoType.DATE10, f7));
//                            response.setField(11, new IsoValue<>(IsoType.NUMERIC, f11, 6));
//                            response.setField(39, new IsoValue<>(IsoType.ALPHA, "06", 2));
//                        }
//                    }
//
//                    sendResponse(response);
//
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//
//            }
//        }
//    };
//
//
//    private ExecutorService subscriberExecutor;
//    private ExecutorService publisherExecutor;
//    private Future subscriberFuture;
//
//
//    @PostConstruct
//    public void init() {
//        System.out.println("init");
//        jedisSubscriber = jedisPool.getResource();
//        jedisPublisher = jedisPool.getResource();
//        if (subscriberFuture == null) {
//            logger.info("Start background thread for getting request from Redis");
//            subscriberExecutor = Executors.newFixedThreadPool(1);
//            subscriberFuture = subscriberExecutor.submit(redisListenerTask);
//            publisherExecutor = Executors.newFixedThreadPool(20);
//        }
//
//        iso8583Server.addMessageListener(this);
//    }
//
//    private final RedisListenerTask redisListenerTask = new RedisListenerTask();
//
//    @PreDestroy
//    public synchronized void destroy() {
//        if (subscriberFuture != null) {
//            try {
//                jedisPubSub.unsubscribe();
//                subscriberExecutor.shutdownNow();
//                publisherExecutor.shutdownNow();
//                logger.info("Background thread for getting request is now closed");
//            } catch (Exception ex) {
//                logger.warn(ex.getMessage());
//                ex.printStackTrace();
//            } finally {
//                try {
//                    jedisPublisher.quit();
//                    jedisSubscriber.quit();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
//
//
//    private class RedisListenerTask implements Runnable {
//
//        public void run() {
//            try {
//                jedisSubscriber.subscribe(jedisPubSub, REDIS_CHANNEL_REQUEST);
//            } catch (Exception ex) {
//                logger.warn(ex.getMessage());
//                ex.printStackTrace();
//            }
//        }
//    }
//
//    private Jedis jedisPublisher;
//    private Jedis jedisSubscriber;
//
//    public void sendResponse(IsoMessage isoMsg) {
//        try {
//            String response = new String(isoMsg.writeData(), StandardCharsets.UTF_8);
//            jedisPublisher.publish(REDIS_CHANNEL_RESPONSE,response);
//        } catch (Exception ex) {
//            logger.warn(ex.getMessage());
//            ex.printStackTrace();
//        }
//    }
//
//    // Response Listener
//    private final Map<String,CountDownLatch> latchMap = new HashMap<>();
//    private final Map<String,IsoMessage> futureResult = new HashMap<>();
//
//    @Override
//    public boolean applies(@NotNull IsoMessage isoMessage) {
//        return isoMessage.getType() == 0x210 || isoMessage.getType() == 0x421;
//    }
//
//    @Override
//    public boolean onMessage(@NotNull ChannelHandlerContext ctx, @NotNull IsoMessage isoMessage) {
//        logger.info("onMessage");
//        String identifier = isoMessage.getField(7).toString()+isoMessage.getField(11).toString();
//        logger.info("lookup  {}",identifier);
//        CountDownLatch latch = latchMap.get(identifier);
//        logger.info("------------ latch {}",latch.toString());
//        logger.warn("welcoming {}",identifier);
//        futureResult.put(identifier,isoMessage);
//        latch.countDown();
//        logger.warn("latch ........ {}",latch.getCount());
//        //ctx.flush();
//        return false;
//    }
//
//
//    private final static Logger logger = LoggerFactory.getLogger(RedisPubSub.class.getName());
//}
//
