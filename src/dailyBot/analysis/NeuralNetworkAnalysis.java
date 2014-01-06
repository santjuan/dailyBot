package dailyBot.analysis;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.encog.engine.network.activation.ActivationLinear;
import org.encog.engine.network.activation.ActivationSigmoid;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataPair;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.neural.data.NeuralDataSet;
import org.encog.neural.data.basic.BasicNeuralDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.Train;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;
import org.encog.util.simple.EncogUtility;

import dailyBot.model.Pair;
import dailyBot.model.Strategy.StrategyId;

public class NeuralNetworkAnalysis
{
    public static void test(double[][] inputValues, double[][] outputValues)
    {
        NeuralDataSet trainingSet = new BasicNeuralDataSet(inputValues, outputValues);
        BasicNetwork network = new BasicNetwork();
        network.addLayer(new BasicLayer(new ActivationSigmoid(), false, 4));
        network.addLayer(new BasicLayer(new ActivationSigmoid(), false, 1000));
        network.addLayer(new BasicLayer(new ActivationLinear(), false, 1));
        network.getStructure().finalizeStructure();
        network.reset();
        final Train train = new ResilientPropagation(network, trainingSet);
        int epoch = 1;
        do
        {
            train.iteration();
            System.out.println("Epoch #" + epoch + " Error:" + train.getError());
            epoch++;
        }
        while(epoch < 10000);
        System.out.println("Neural Network Results:");
        for(MLDataPair pair : trainingSet)
        {
            final MLData output = network.compute(pair.getInput());
            System.out.println(pair.getInput().getData(0) + "," + pair.getInput().getData(1) + ", actual="
                + output.getData(0) + ",ideal=" + pair.getIdeal().getData(0));

        }
    }

    static String print(double[] input, double[] output)
    {
        String ans = "";
        for(double d : input)
            ans += " " + d;
        for(double d : output)
            ans += " " + d;
        return ans.trim() + '\n';
    }

    private static BasicNetwork generateNeuralNetwork(StrategyId id, Pair currency, boolean isBuy, int iterations,
        int middleNeurons) throws IOException
    {
        List<SignalHistoryRecord> allEntries = Utils.getStrategyRecords(id, currency);
        Collections.shuffle(allEntries);
        int validationSize = (int) Math.round(allEntries.size() * 0.2);
        double[][] inputs = new double[allEntries.size() - validationSize][];
        double[][] outputs = new double[allEntries.size() - validationSize][];
        double[][] inputsValidation = new double[validationSize][];
        double[][] outputsValidation = new double[validationSize][];
        int index = 0;
        int indexValidation = 0;
        for(SignalHistoryRecord entry : allEntries)
        {
            if(index < inputs.length)
            {
                inputs[index] = entry.getCharacteristics();
                outputs[index++] = entry.getOutput();
            }
            else
            {
                inputsValidation[indexValidation] = entry.getCharacteristics();
                outputsValidation[indexValidation++] = entry.getOutput();
            }
        }
        test(inputs, outputs);
        BasicNetwork network = EncogUtility.simpleFeedForward(inputs[0].length, 50, 0, outputs[0].length, true);
        MLDataSet trainingSet = new BasicMLDataSet(inputs, outputs);
        System.out.println("Neural Network Results:");
        EncogUtility.trainConsole(network, trainingSet, 1);
        for(int i = 0; i < inputs.length; i++)
        {
            final MLData output = network.compute(new BasicMLData(inputs[i]));
            System.out.println(Arrays.toString(inputs[i]) + ", actual=" + output.getData(0) + ", ideal="
                + outputs[i][0]);

        }
        System.out.println("Validation:");
        System.out.println("Validation:");
        System.out.println("Validation:");
        System.out.println("Validation:");
        for(int i = 0; i < inputsValidation.length; i++)
        {
            final MLData output = network.compute(new BasicMLData(inputsValidation[i]));
            System.out.println(Arrays.toString(inputsValidation[i]) + ", actual=" + output.getData(0) + ", ideal="
                + outputsValidation[i][0]);

        }
        return network;
    }

    public static void main(String[] args) throws IOException
    {
        generateNeuralNetwork(StrategyId.MOMENTUM2, Pair.EURAUD, true, 1000, 100);
    }
}