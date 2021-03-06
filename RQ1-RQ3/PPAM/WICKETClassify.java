import weka.core.converters.ConverterUtils.DataSource;
import weka.core.Instances;
import weka.classifiers.Classifier;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Evaluation;
import java.util.Random;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.Filter;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.*;

public class WICKETClassify {

   public static void main(String[] args) throws Exception{
      DataSource sourceTrain = new DataSource("PPAMs/WICKETPPAMTrainSet.csv");
      DataSource sourceTest = new DataSource("PPAMs/WICKETPPAMTestSet.csv");

      Instances originaldataTrain = sourceTrain.getDataSet();
      Instances originaldataTest = sourceTest.getDataSet();

      int [] indices = new int[]{3,4,5};
      Remove removeFilter1 = new Remove();
      removeFilter1.setAttributeIndicesArray(indices);
      removeFilter1.setInvertSelection(true);
      removeFilter1.setInputFormat(originaldataTrain);
      Instances dataTrain = Filter.useFilter(originaldataTrain, removeFilter1);
      Remove removeFilter2 = new Remove();
      removeFilter2.setAttributeIndicesArray(indices);
      removeFilter2.setInvertSelection(true);
      removeFilter2.setInputFormat(originaldataTest);
      Instances dataTest = Filter.useFilter(originaldataTest, removeFilter1);

      if (dataTrain.classIndex() == -1)
         dataTrain.setClassIndex(dataTrain.numAttributes() - 1);
      if (dataTest.classIndex() == -1)
         dataTest.setClassIndex(dataTrain.numAttributes() - 1);

      AttributeSelection as = new AttributeSelection();
      ASSearch asSearch = ASSearch.forName("weka.attributeSelection.GreedyStepwise", new String[]{"-C", "-R"});
      as.setSearch(asSearch);
      ASEvaluation asEval = ASEvaluation.forName("weka.attributeSelection.CfsSubsetEval", new String[]{"-M"});
      as.setEvaluator(asEval);
      as.SelectAttributes(dataTrain);
      dataTrain = as.reduceDimensionality(dataTrain);
      Classifier classifier = AbstractClassifier.forName("weka.classifiers.trees.J48", new String[]{"-O", "-J", "-S", "-M", "1", "-C", "0.17422854839431923"});
      classifier.buildClassifier(dataTrain);

      Evaluation eval = new Evaluation(dataTrain);
      eval.evaluateModel(classifier, dataTest);
      System.out.println(eval.toSummaryString("\nResults\n======\n", false));
      System.out.println(eval.toClassDetailsString());

      Long TP = 0L, TN = 0L, FP = 0L, FN = 0L;
      // label dataTrain
      for (int i = 0; i < dataTest.numInstances(); i++) {
         double clsLabel = classifier.classifyInstance(dataTest.instance(i));
         String actual = dataTest.classAttribute().value((int) dataTest.instance(i).classValue());
         String predicted = dataTest.classAttribute().value((int) clsLabel);
         if (actual.equals("Yes")) {
            if (predicted.equals("Yes"))
               TP++;
            else
               FN++;
         }
         else {
            if (predicted.equals("Yes"))
               FP++;
            else
               TN++;
         }
      }
      Double precision = (double)TP /(double)(TP + FP);
      Double recall = (double)TP /(double)(TP + FN);
      Double F1 = (double)(precision * recall *2) / (double)(precision + recall);
      Double MCC = (double)(TP * TN - FP * FN) / (Math.sqrt((TP + FP) * (FN + TN) * (FP + TN) * (TP + FN)));
      Double observed = (double)(TP + TN) / (double)(TP+ FN + FP + TN);
      Double a = (double)((TP + FN) * (TP + FP)) / (double)(TP+ FN + FP + TN);
      Double b = (double)((FP + TN) * (FN + TN)) / (double)(TP+ FN + FP + TN);
      Double expected = (a + b) / (double)(TP+ FN + FP + TN);
      Double kappa = (observed - expected)/(1 - expected);
      System.out.println( "Precision" + " " + precision.toString() + "\nRecall" + recall.toString()
                  + "\nF1" + F1.toString() +"\nMCC" + MCC.toString() + "\nKappa" + kappa.toString() + "\n");
   }

}
