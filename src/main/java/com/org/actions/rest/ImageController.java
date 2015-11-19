package com.org.actions.rest;

import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageFloat32;
import com.opensymphony.xwork2.ModelDriven;
import org.apache.struts2.rest.DefaultHttpHeaders;
import org.apache.struts2.rest.HttpHeaders;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import org.apache.commons.codec.binary.Base64;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;


/**
 * Created by ericbhatti on 11/18/15.
 */
public class ImageController implements ModelDriven<Object> {

    private static final long serialVersionUID = 89268917575477696L;
    ImageTest imageTest = new ImageTest();
    Matcher matcher = new Matcher();
    LocationHelper locationHelper = LocationHelper.getInstance();
    String[] classes = {"Faculty_Room_1", "Faculty_Room_2"};

    public HttpHeaders create() throws Exception {

        long start = System.currentTimeMillis();
//        TupleDesc[] features = getFeaturesFromImage(imageTest.getData());
//        Instances testingInstances = getInstances(features);
        Instances testingInstances = getInstances(getFeaturesFromImage(imageTest.getData()));
        testingInstances.setClassIndex(testingInstances.numAttributes() - 1);
        String response[] = getClass(testingInstances);
        long end = System.currentTimeMillis();
        System.out.println("TIME: " + String.valueOf(end - start));
        imageTest.setData(response[0]);
        imageTest.setLocation(response[1]);
        return new DefaultHttpHeaders("create");
    }


    private TupleDesc[] getFeaturesFromImage(String base64) throws Exception {

        byte[] data = Base64.decodeBase64(base64);
        BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(data));
        ImageFloat32 float32 = new ImageFloat32(320, 240);
        ConvertBufferedImage.convertFrom(bufferedImage, float32);
        return matcher.detectDescribe(float32);
    }

    private String[] getClass(Instances instances) throws Exception {
        int[] classCount = new int[classes.length];
        classCount[0] = 0;
        classCount[1] = 0;
        for (int i = 0; i < instances.numInstances(); i++) {
            Instance instance = instances.instance(i);
            int output = (int) locationHelper.getModel().classifyInstance(instance);
            classCount[output]++;
        }
        System.out.println(String.valueOf(classCount[0]) + "  " + String.valueOf(classCount[1] + " " + String.valueOf(instances.numInstances())));
        if (classCount[0] > classCount[1]) {
            return new String[]{String.valueOf(classCount[0]) + "  " + String.valueOf(classCount[1]), classes[0]};
        } else if (classCount[0] < classCount[1]) {
            return new String[]{String.valueOf(classCount[0]) + "  " + String.valueOf(classCount[1]), classes[1]};
        } else {
            return new String[]{String.valueOf(classCount[0]) + "  " + String.valueOf(classCount[1]), ""};
        }

    }

    private Instances getInstances(TupleDesc[] features) throws Exception {
        StringBuilder dataBuilder = new StringBuilder(locationHelper.getARFF_Header().toString());
        for (TupleDesc element : features) {
            for (int i = 0; i < element.size(); i++) {
                if (allzeros(element)) continue;
                dataBuilder.append(element.getDouble(i) + ",");
                if (i == element.size() - 1) { //
                    dataBuilder.append("?\n");
                }
            }
        }
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(dataBuilder.toString().getBytes());
        ConverterUtils.DataSource source = new ConverterUtils.DataSource(byteArrayInputStream);
        return source.getDataSet();
    }

    public byte[] hexStringToByteArray(String s) {
        s = s.replace(" ", "");
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private boolean allzeros(TupleDesc desc) {
        boolean allzero = true;
        for (int i = 0; i < desc.size(); i++) {
            if (desc.getDouble(i) != 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Object getModel() {
        return imageTest;
    }

    public HttpHeaders index() throws IOException {
        InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("testing.txt");
        InputStreamReader is = new InputStreamReader(input);
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(is);
        String read = br.readLine();

        while (read != null) {
            //System.out.println(read);
            sb.append(read);
            read = br.readLine();

        }
        imageTest.setData(sb.toString());
        imageTest.setLocation("The world is now!");
        return new DefaultHttpHeaders("index").disableCaching();
    }
}
