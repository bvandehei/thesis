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
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.IOException;

public class LOG4J2Combined {
   public static HashMap<String, Integer> VersionAndIndex;
   public static HashMap<String, Integer> BugAndFV;
   public static HashMap<String, Integer> BugAndCreation;

   public static void main(String[] args) throws Exception{
      String linefrombug;
      String cvsSplitBy = ",";
      String bugfilename = "../../RQ3-AllAVRetrievalMethods/CreateInputFiles/OrderedBugOutputFiles/"
                                    + "LOG4J2" + "BugInfoOrdered.csv";
         //Get Bug Info for each project
      try (BufferedReader brBugs = new BufferedReader(new FileReader(bugfilename))) {
         BugAndCreation = new HashMap<String, Integer>();
         BugAndFV = new HashMap<String, Integer>();
         brBugs.readLine();
         while ( (linefrombug = brBugs.readLine()) != null) {
            String[] tokenBug = linefrombug.split(cvsSplitBy);
            BugAndCreation.put(tokenBug[0], Integer.parseInt(tokenBug[4]));
            BugAndFV.put(tokenBug[0], Integer.parseInt(tokenBug[5]));
         }
      } catch (IOException e) {
         e.printStackTrace();
      }

      String versionfilename = "../../RQ3-AllAVRetrievalMethods/CreateInputFiles/VersionOutputFiles/"
                                    + "LOG4J2" + "VersionInfo.csv";
         //Get Bug Info for each project
      try (BufferedReader br = new BufferedReader(new FileReader(versionfilename))) {
         VersionAndIndex = new HashMap<String, Integer>();
         br.readLine();
         Integer last = 0;
         while ( (linefrombug = br.readLine()) != null) {
            String[] token = linefrombug.split(cvsSplitBy);
            last = Integer.parseInt(token[0]);
            VersionAndIndex.put(token[2], last);
         }
         VersionAndIndex.put("not released", last + 1);
      } catch (IOException e) {
         e.printStackTrace();
      }

      DataSource sourceTrain = new DataSource("../CombinedFiles/LOG4J2CombinedTrainSet.csv");
      DataSource sourceTest = new DataSource("../CombinedFiles/LOG4J2CombinedTestSet.csv");

      Instances originaldataTrain = sourceTrain.getDataSet();
      Instances originaldataTest = sourceTest.getDataSet();

      int [] indices = new int[]{0,1,2,3,5,6,7,8,9,10};
      Remove removeFilter1 = new Remove();
      removeFilter1.setAttributeIndicesArray(indices);
      removeFilter1.setInputFormat(originaldataTrain);
      Instances dataTrain = Filter.useFilter(originaldataTrain, removeFilter1);
      Remove removeFilter2 = new Remove();
      removeFilter2.setAttributeIndicesArray(indices);
      removeFilter2.setInputFormat(originaldataTest);
      Instances dataTest = Filter.useFilter(originaldataTest, removeFilter1);

      if (dataTrain.classIndex() == -1)
         dataTrain.setClassIndex(dataTrain.numAttributes() - 1);
      if (dataTest.classIndex() == -1)
         dataTest.setClassIndex(dataTrain.numAttributes() - 1);

      AttributeSelection as = new AttributeSelection();
      ASSearch asSearch = ASSearch.forName("weka.attributeSelection.BestFirst", new String[]{"-D", "1", "-N", "9"});
      as.setSearch(asSearch);
      ASEvaluation asEval = ASEvaluation.forName("weka.attributeSelection.CfsSubsetEval", new String[]{"-M", "-L"});
      as.setEvaluator(asEval);
      as.SelectAttributes(dataTrain);
      dataTrain = as.reduceDimensionality(dataTrain);
      Classifier classifier = AbstractClassifier.forName("weka.classifiers.rules.OneR", new String[]{"-B", "6"});
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
         String bug = originaldataTest.instance(i).stringValue(0);
         String versionName = originaldataTest.instance(i).stringValue(1);
         Integer versionIndex = VersionAndIndex.get(versionName);
         Integer fix = BugAndFV.get(bug);
         Integer ov = BugAndCreation.get(bug);
         if (versionIndex >= ov && versionIndex < fix) {
            System.out.println(predicted);
            predicted = "Yes";
         }
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
