package id.co.nio;

import com.github.kpavlov.jreactive8583.IsoMessageListener;
import com.github.kpavlov.jreactive8583.client.ClientConfiguration;
import com.github.kpavlov.jreactive8583.client.Iso8583Client;
import com.github.kpavlov.jreactive8583.iso.ISO8583Version;
import com.github.kpavlov.jreactive8583.iso.J8583MessageFactory;
import com.solab.iso8583.IsoMessage;
import com.solab.iso8583.IsoType;
import com.solab.iso8583.IsoValue;
import com.solab.iso8583.MessageFactory;
import com.solab.iso8583.parse.ConfigParser;
import io.netty.channel.ChannelHandlerContext;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@ServletComponentScan
public class ISOClient {
    public static final Logger logger = LoggerFactory.getLogger(ISOClient.class);

    private final static int port = 7501;
    private final static String host = "localhost";

    public static void main(String[] args) throws Exception {
        SpringApplication.run(ISOClient.class, args);
    }

    @Bean
    Iso8583Client<IsoMessage> iso8583Client()  {
        try {
            MessageFactory<IsoMessage> mf = new MessageFactory<>();
            ConfigParser.configureFromClasspathConfig(mf, "j8583.xml");
            J8583MessageFactory<IsoMessage> messageFactory = new J8583MessageFactory<>(mf, ISO8583Version.V1987);// [1]

            ClientConfiguration clientConfiguration = ClientConfiguration.newBuilder()
                    .addLoggingHandler(false)
                    .logSensitiveData(false)
                    .replyOnError(true)
                    .workerThreadsCount(120)
                    .idleTimeout(0)
                    .build();

            SocketAddress targetServer = new InetSocketAddress(host, port);
            final Iso8583Client<IsoMessage> client = new Iso8583Client<>(targetServer, clientConfiguration, messageFactory);// [2]

            client.addMessageListener(new IsoMessageListener<IsoMessage>() {
                public boolean onMessage(@NotNull ChannelHandlerContext ctx, @NotNull IsoMessage isoMessage) {
                    //System.out.println("onMessage");
                    //capturedRequests.add(isoMessage);
                    IsoMessage response = client.getIsoMessageFactory().createResponse(isoMessage);
                    response.setField(39, IsoType.ALPHA.value("01", 2));
//                    ctx.flush();
                    ctx.writeAndFlush(response);
                    return false;
                }

                //public boolean applies(@NotNull IsoMessage isoMessage) {
//                    return isoMessage.getType() == 0x800;
//                } // [3]
                public boolean applies(@NotNull IsoMessage isoMessage) {
                    return isoMessage.getType() == 0x800 || isoMessage.getType() == 0x200 ;
                }
            });

            client.init();

            client.connect();// [6]

            do {
                System.out.println("ding dong");

                IsoMessage isoMessage = client.getIsoMessageFactory().newMessage(0x800);

                isoMessage.setField(2, new IsoValue<>(IsoType.LLVAR,"01"));
                isoMessage.setField(7, new IsoValue<>(IsoType.DATE10,"1212121212"));
                isoMessage.setField(11, new IsoValue<>(IsoType.NUMERIC,"111111",6));
                isoMessage.setField(33, new IsoValue<>(IsoType.LLVAR,"232323"));
                isoMessage.setField(48, new IsoValue<>(IsoType.LLLVAR,"555555"));
                isoMessage.setField(70, new IsoValue<>(IsoType.NUMERIC,"301",3));

                System.out.println("sending isomsg");
                client.send(isoMessage,10000, TimeUnit.MILLISECONDS);

            } while (!client.isConnected());


            return client;

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(3);
        }
        return null;
    }
}
