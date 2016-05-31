package com.alphatica.genotick.genotick;

import com.alphatica.genotick.data.DataUtils;
import com.alphatica.genotick.data.YahooFixer;
import com.alphatica.genotick.population.Population;
import com.alphatica.genotick.population.PopulationDAOFileSystem;
import com.alphatica.genotick.population.Robot;
import com.alphatica.genotick.population.RobotInfo;
import com.alphatica.genotick.reversal.Reversal;
import com.alphatica.genotick.ui.Parameters;
import com.alphatica.genotick.ui.UserInput;
import com.alphatica.genotick.ui.UserInputOutputFactory;
import com.alphatica.genotick.ui.UserOutput;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main {
    public static final String DEFAULT_DATA_DIR = "data";
    public static Random random;
    private static final String VERSION = "Genotick version 0.10.1 (copyleft 2016)";
    private static UserInput input;
    private static UserOutput output;

    public static void main(String... args) {
        setupDebug();
        assignRandom();
        setupExceptionHandler();
        Parameters parameters = new Parameters(args);
        checkVersionRequest(parameters);
        checkShowPopulation(parameters);
        checkShowRobot(parameters);
        getUserIO(parameters);
        checkReverse(parameters);
        checkYahoo(parameters);
        checkSimulation(parameters);
    }

    private static void assignRandom() {
        random = new Random();
        String seedString = System.getenv("GENOTICK_RANDOM_SEED");
        if( seedString != null) {
            long seed = Long.parseLong(seedString);
            random.setSeed(seed);
            Debug.d("Assigning",seed,"to random seed. Testing only!");
        }
    }

    private static void checkShowRobot(Parameters parameters) {
        String value = parameters.getValue("showRobot");
        if(value != null) {
            try {
                showRobot(value);
            } catch (IllegalAccessException e) {
                Debug.d(e);
            }
            System.exit(0);
        }
    }

    private static void showRobot(String value) throws IllegalAccessException {
        String robotString = getRobotString(value);
        System.out.println(robotString);
    }

    private static String getRobotString(String path) throws IllegalAccessException {
        File file = new File(path);
        Robot robot = PopulationDAOFileSystem.getRobotFromFile(file);
        return robot.showRobot();
    }

    private static void checkShowPopulation(Parameters parameters) {
        String value = parameters.getValue("showPopulation");
        if(value != null) {
            try {
                showPopulation(value);
            } catch (IllegalAccessException e) {
                Debug.d(e);
            }
            System.exit(0);
        }
    }

    private static void showPopulation(String path) throws IllegalAccessException {
        PopulationDAOFileSystem dao = new PopulationDAOFileSystem(path);
        Population population = PopulationFactory.getDefaultPopulation(dao);
        showHeader();
        showRobots(population);
    }

    private static void showRobots(Population population) throws IllegalAccessException {
        for(RobotInfo robotInfo: population.getRobotInfoList()) {
            String info = getRobotInfoString(robotInfo);
            System.out.println(info);
        }
    }

    private static String getRobotInfoString(RobotInfo robotInfo) throws IllegalAccessException {
        StringBuilder sb = new StringBuilder();
        Field [] fields = robotInfo.getClass().getDeclaredFields();
        for(Field field: fields) {
            field.setAccessible(true);
            if(!Modifier.isStatic(field.getModifiers())) {
                Object object = field.get(robotInfo);
                if(sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(object.toString());
            }
        }
        return sb.toString();
    }

    private static void showHeader() {
        Class infoClass = RobotInfo.class;
        List<Field> fields = buildFields(infoClass);
        String line = buildLine(fields);
        System.out.println(line);
    }

    private static List<Field> buildFields(Class infoClass) {
        List<Field> fields = new ArrayList<>();
        for(Field field: infoClass.getDeclaredFields()) {
            if(!Modifier.isStatic(field.getModifiers())) {
                fields.add(field);
            }
        }
        return fields;
    }

    private static String buildLine(List<Field> fields) {
        StringBuilder sb = new StringBuilder();
        for (Field field : fields) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(field.getName());
        }
        return sb.toString();
    }

    private static void setupExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(Debug.createExceptionHandler());
    }

    private static void checkVersionRequest(Parameters parameters) {
        if(parameters.getValue("showVersion") != null) {
            System.out.println(Main.VERSION);
            System.exit(0);
        }
    }

    private static void checkYahoo(Parameters parameters) {
        String yahooValue = parameters.getValue("fixYahoo");
        if(yahooValue == null) {
            return;
        }
        YahooFixer yahooFixer = new YahooFixer(yahooValue);
        yahooFixer.fixFiles();
        System.exit(0);
    }

    private static void setupDebug() {
        Debug.setShowTime(true);
        String outFileName = "genotick_" + DataUtils.getDateTimeString() + ".txt";
        Debug.toFile(outFileName);
    }

    private static void getUserIO(Parameters parameters) {
        input = UserInputOutputFactory.getUserInput(parameters);
        if(input == null) {
            exit(errorCodes.NO_INPUT);
        }
        output = UserInputOutputFactory.getUserOutput(parameters);
        //noinspection ConstantConditions
        if(output == null) {
            exit(errorCodes.NO_OUTPUT);
        }
    }

    private static void checkReverse(Parameters parameters) {
        String reverseValue = parameters.getValue("reverse");
        if(reverseValue == null)
            return;
        Reversal reversal = new Reversal(reverseValue,output);
        reversal.reverse();
        System.exit(0);
    }

    private static void checkSimulation(Parameters parameters) {
        if(!parameters.allConsumed()) {
            output.errorMessage("Not all arguments processed: " + parameters.getUnconsumed());
            exit(errorCodes.UNKNOWN_ARGUMENT);
        }
        Application application = new Application(input, output);
        input.show(application);
    }

    private static void exit(errorCodes code) {
        System.exit(code.getValue());
    }
}

