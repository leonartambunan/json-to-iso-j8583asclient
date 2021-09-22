//package id.co.nio;
//
//import com.github.kpavlov.jreactive8583.IsoMessageListener;
//import com.github.kpavlov.jreactive8583.iso.ISO8583Version;
//import com.github.kpavlov.jreactive8583.iso.J8583MessageFactory;
//import com.github.kpavlov.jreactive8583.server.Iso8583Server;
//import com.github.kpavlov.jreactive8583.server.ServerConfiguration;
//import com.solab.iso8583.IsoMessage;
//import com.solab.iso8583.IsoType;
//import com.solab.iso8583.MessageFactory;
//import com.solab.iso8583.parse.ConfigParser;
//import io.netty.channel.ChannelHandlerContext;
//import org.jetbrains.annotations.NotNull;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.boot.web.servlet.ServletComponentScan;
//import org.springframework.context.annotation.Bean;
//
//
//@SpringBootApplication
////@ServletComponentScan
//public class ISOServer {
//    public static final Logger logger = LoggerFactory.getLogger(ISOServer.class);
//    private final static int port = 9999;
//
//    public static void main(String[] args) throws Exception {
//        SpringApplication.run(ISOServer.class, args);
//    }
//
//    public static ChannelHandlerContext channelHandlerContext=null;
//
//    @Bean
//    Iso8583Server<IsoMessage> iso8583Server() throws Exception {
//        MessageFactory mf = new MessageFactory();
//        ConfigParser.configureFromClasspathConfig(mf,"j8583.xml");
//        J8583MessageFactory messageFactory = new J8583MessageFactory<>(mf, ISO8583Version.V1987);// [1]
//
//        ServerConfiguration serverConfiguration = ServerConfiguration.newBuilder()
//                .addLoggingHandler(true)
//                .logSensitiveData(true)
//                .workerThreadsCount(4)
//                .replyOnError(true)
//                .workerThreadsCount(12)
//                .idleTimeout(0)
//                .build();
//
//        final Iso8583Server<IsoMessage> server = new Iso8583Server<IsoMessage>(port,serverConfiguration, messageFactory);// [2]
//
//        server.addMessageListener(new IsoMessageListener<IsoMessage>() {
//            public boolean onMessage(@NotNull ChannelHandlerContext ctx, @NotNull IsoMessage isoMessage) {
//
//                IsoMessage response = server.getIsoMessageFactory().createResponse(isoMessage);
//
//                if ("061".equalsIgnoreCase(isoMessage.getField(70).toString())) {
//                    channelHandlerContext = ctx;
//                }
//
//                response.setField(39, IsoType.ALPHA.value("00", 2));
//
//                logger.info("------------------");
//                logger.info(response.debugString());
//
//                ctx.writeAndFlush(response);
//
//                return false;
//            }
//
//            public boolean applies(@NotNull IsoMessage isoMessage) {
//                return isoMessage.getType() == 0x800;
//            } // [3]
//
//
//        });
//
//        serverConfiguration.replyOnError();
//        server.init();
//
//        server.start();// [6]
//        if (server.isStarted()) { // [7]
//            logger.info("ding dong");
//        }
//
//        return server;
//    }
//}
