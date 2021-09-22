package id.co.nio;

import com.solab.iso8583.IsoType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.xml.sax.InputSource;

import javax.annotation.PostConstruct;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import java.io.File;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;

@Component
public class PackagerConfig {

    public static Map<Integer, IsoType> isoTypeMap = new HashMap<>();
    public static Map<Integer, Integer> isoLengthMap = new HashMap<>();


    @PostConstruct
    public void process() throws Exception{

        String fileName = "j8583.xml";

        String fullFilePath = this.getClass().getClassLoader().getResource(".").getPath()+fileName;

        System.out.println(fullFilePath);

        String fileContent = readFile(new File(fullFilePath));

        J8583Config isopackager = unmarshal(fileContent);

        for (Parse o : isopackager.parse)
            if ("0200".equalsIgnoreCase(o.type)) {
                for (Field  f: o.field) {
                    System.out.println(f.num);
                    System.out.println(f.type);
                    System.out.println(f.length);
                    Integer field = Integer.valueOf(f.num);
                    Integer length = StringUtils.isEmpty(f.length)?null:Integer.valueOf(f.length);
                    IsoType isoType = IsoType.valueOf(f.type);
                    PackagerConfig.isoLengthMap.put(field,length);
                    PackagerConfig.isoTypeMap.put(field,isoType);
                }
            }
    }

    public String readFile(File file) throws Exception {
        StringBuilder stringBuilder = new StringBuilder();
        LineIterator it = FileUtils.lineIterator(file, "UTF-8");
        try {
            while (it.hasNext()) {
                String line = it.nextLine();
                stringBuilder.append(line);
                stringBuilder.append("\n");
            }
        } finally {
            it.close();
        }

        return stringBuilder.toString();
    }

    public J8583Config unmarshal(String fileName) throws Exception {
        //Disable XXE
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        spf.setValidating(false);
        spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
        //Do unmarshall operation
        Source xmlSource = new SAXSource(spf.newSAXParser().getXMLReader(),
                new InputSource(new StringReader(fileName)));

        JAXBContext jc = JAXBContext.newInstance(J8583Config.class);

        Unmarshaller um = jc.createUnmarshaller();
        um.setSchema(null);
        return (J8583Config) um.unmarshal(xmlSource);
    }


}
