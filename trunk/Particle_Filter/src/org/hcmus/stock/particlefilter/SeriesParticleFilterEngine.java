package org.hcmus.stock.particlefilter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class SeriesParticleFilterEngine 
{
	public SeriesParticleFilterEngine()
	{}
	
	public ArrayList<Double> functionGeneration(double anchorValue, double referenceValue, int numParticle, int numLoop, double threshold, double riseAndFallIndicator)
	{
		ArrayList<ArrayList<Double>> distributionList = new ArrayList<ArrayList<Double>>();
		
		ArrayList<Double> errorList = new ArrayList<Double>();
		
		Random primary = new Random();
		
		for(int loop = 0; loop < numLoop; loop++)
		{
			double functionError = 0;
			ArrayList<Double> distribution = new ArrayList<Double>();
			ArrayList<Double> priceList = new ArrayList<Double>();
			
			Random valueDist = new Random(primary.nextInt());
			
			for(int i = 0; i < numParticle; i++)
			{
				Random riseFallDist = new Random(primary.nextInt());
				
				double differences = 0;
				
				double probability = riseFallDist.nextDouble();
				
				if(probability > riseAndFallIndicator)
				{
					probability = valueDist.nextDouble();
					
					distribution.add(probability);
					
					differences = anchorValue * 0.05 * probability;
				}
				else
				{
					probability = - valueDist.nextDouble();
					
					distribution.add(probability);
					
					differences = anchorValue * 0.05 * probability;
				}
				
				priceList.add(anchorValue + differences);
			}
			
			for(int i = 0; i < numParticle; i++)
			{
				functionError += Math.abs(priceList.get(i) - referenceValue);
			}
			
			functionError = functionError / numParticle;
			
			if(functionError < threshold)
				return distribution;
			
			distributionList.add(distribution);
			errorList.add(functionError);
		}
		
		return distributionList.get(errorList.indexOf(Collections.min(errorList)));
	}

	public double predictValue(int startDate, int endDate, int numParticle, int numLoop, double threshold, ArrayList<Double> data)
	{
		double result = 0;
		
		ArrayList<Double> distribution = new ArrayList<Double>();
	
		//Actually the initial i must take value startDate - 1, but due to the fact that we need
		//a pair of value for the functionGeneration module to work then we shift the initially to 1
		//We can see in the anchorValue of the functionGeneration module we use Data.get(i - 1), that is the actual startDate
		for(int i = startDate; i < endDate; i++)
		{
			ArrayList<Double> distributionElement = functionGeneration(data.get(i - 1), data.get(i), numParticle, numLoop, threshold, 0.5);
			
			distribution.addAll(distributionElement);
			
			//Bug tracking
			//System.out.println(data.get(i - 1) + " " + data.get(i));
		}
		
		ArrayList<Double> weightList = normalizer(weighting(distribution, 0.1));
		
		double distance = 0;
		
		for(int i = 0; i < distribution.size(); i++)
		{
			distance += data.get(endDate - 1) * 0.05 * distribution.get(i) * weightList.get(i);
		}
		
//		Random randomWalk = new Random();
//		
//		if(randomWalkIndicator(startDate, endDate, data))
//		{
//			if(randomWalk.nextDouble() > 0.5)
//			{
//				distance = -distance;
//			}
//		}
		
		result = data.get(endDate - 1) + distance;
		
		//Bug tracking
		//System.out.println(data.get(endDate - 1) + "\t" + distance + "\t" + result);
		
		return result;
	}
	
	public ArrayList<Double> normalizer(ArrayList<Double> inputData)
	{
		ArrayList<Double> result = new ArrayList<Double>();
		
		double sum = 0;
		
		for(int i = 0; i < inputData.size(); i++)
		{
			sum += inputData.get(i);
		}
		
		for(int i = 0; i < inputData.size(); i++)
		{
			result.add(inputData.get(i)/sum);
		}
		
		return result;
	}
	
	public ArrayList<Double> weighting(ArrayList<Double> data, double distance)
	{
		ArrayList<Double> result = new ArrayList<Double>();
		
		for(int i = 0; i < data.size(); i++)
		{
			double weight = 1;
			for(int j = 0; j < data.size(); j++)
			{
				if(i != j)
				{
					if(Math.abs(data.get(i) - data.get(j)) <= distance)
					{
						weight++;
					}
				}
			}
			
			result.add(weight);			
		}
		
		return result;
	}
	
	public double functionGenerationStatistic(double anchorValue, double referenceValue, int numParticle, int loopNum, double riseAndFallIndicator)
	{
		ArrayList<Double> particleList = new ArrayList<Double>();
	
		ArrayList<Double> errorList = new ArrayList<Double>();
		
		for(int loop = 0; loop < loopNum; loop++)
		{
			double functionError = 0;
			
			//Generate particle list
			for(int i = 0; i < numParticle; i++)
			{
				Random generator = new Random();
				
				double differences = 0;
				
				double probability = generator.nextDouble();
				
				//riseAndFallIndicator variable help to determine in which case the probability make the price rise (or fall)
				if(probability > riseAndFallIndicator)
				{
					differences = anchorValue * 0.05 * probability;
				}
				else
				{
					differences = - (anchorValue * 0.05 * (1 - probability));
				}
				
				particleList.add(anchorValue + differences);
			}
			
			//Get the error of the list;
			for(int i = 0; i < numParticle; i++)
			{
				functionError += Math.abs(particleList.get(i) - referenceValue);
			}
			
			functionError = functionError / numParticle;
			
			errorList.add(functionError);
			
			particleList.clear();
		}
		
		double errorMin = Collections.min(errorList);
		
		return errorMin;
	}
	
	public double[] functionGenerationThresholdTesting(double anchorValue, double referenceValue, int numParticle, double threshold, double riseAndFallIndicator)
	{
		ArrayList<Double> particleList = new ArrayList<Double>();
		
		//ArrayList<Double> errorList = new ArrayList<Double>();
		
		double[] result = new double[2]; 
		
		double functionError = Double.MAX_VALUE;
		
		int numLoop = 0;
		
		double minError = Double.MAX_VALUE;
		
		while(functionError > threshold)
		{	
			functionError = 0;
			
			if(numLoop > 300)
			{
				result[0] = minError;
				result[1] = 0; 
				return result;
			}
				
			//Generate particle list
			for(int i = 0; i < numParticle; i++)
			{	
				Random generator = new Random();
				
				double differences = 0;
				
				double probability = generator.nextDouble();
				
				//riseAndFallIndicator variable help to determine in which case the probability make the price rise (or fall)
				if(probability > riseAndFallIndicator)
				{
					differences = anchorValue * 0.05 * probability;
				}
				else
				{
					differences = - (anchorValue * 0.05 * (1 - probability));
				}
				
				particleList.add(anchorValue + differences);
			}
			
			//Get the error of the list;
			for(int i = 0; i < numParticle; i++)
			{
				functionError += Math.abs(particleList.get(i) - referenceValue);
			}
			
			functionError = functionError / numParticle;
			
			if(functionError < minError)
				minError = functionError;
			
			numLoop++;
			
			particleList.clear();
		}
		
		result[0] = functionError;
		result[1] = numLoop;
		
		return result;
	}
	
	public static ArrayList<Double> readFileToDouble(String filePath)
	{
		ArrayList<Double> arrList = new ArrayList<Double>();
		
		try 
		{	
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8"));
			
			String line = null;
			
			double value = 0;
			
			while((line = reader.readLine()) != null)
			{
				value = Double.parseDouble(line);
				arrList.add(value);
			}
			
			reader.close();
		} 
		catch (UnsupportedEncodingException e) 
		{
			e.printStackTrace();
		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
		return arrList;
	}
	
	public static double meanOfList(ArrayList<Double> inputList)
	{
		double result = 0;
		
		for(int i = 0; i < inputList.size(); i++)
		{
			result += inputList.get(i);
		}
		
		result = result / inputList.size();
		
		return result;
	}
	
	public static double stddevOfList(ArrayList<Double> inputList)
	{
		double result = 0;
		
		double mean = meanOfList(inputList);
		
		for(int i = 0; i < inputList.size(); i++)
		{
			result += Math.pow((inputList.get(i) - mean), 2);
		}
		
		result = Math.sqrt((result / inputList.size()));
		
		return result;
	}

	public ArrayList<ArrayList<Double>> parameterTraining(ArrayList<Double> trainingData, ArrayList<int[]> setupList)
	{
		ArrayList<ArrayList<Double>> result = new ArrayList<ArrayList<Double>>();
		
		ArrayList<Double> vError = new ArrayList<Double>();
		ArrayList<Double> tError = new ArrayList<Double>();
		
		for(int iSetup = 0; iSetup < setupList.size(); iSetup++)
		{
			/*System.out.println("Training Setting: " + (iSetup + 1) + " " + 
					setupList.get(iSetup)[0] + " " +
					setupList.get(iSetup)[1] + " " +
					setupList.get(iSetup)[2]);
			*/
			double valueError = 0;
			double trendError = 0;
			
			for(int i = setupList.get(iSetup)[2]; i < trainingData.size(); i++)
			{	
				double predictedValue = predictValue(i - (setupList.get(iSetup)[2] - 1), i, setupList.get(iSetup)[0], setupList.get(iSetup)[1], 1, trainingData);
				
				valueError += Math.abs(predictedValue - trainingData.get(i));
				
				//System.out.println(predictedValue);
				//System.out.println(trainingData.get(i));
				
				if((predictedValue - trainingData.get(i - 1)) * (trainingData.get(i) - trainingData.get(i - 1)) < 0)
				{
					trendError++;
				}
			}
			
			valueError = valueError/(trainingData.size() - setupList.get(iSetup)[2]);
			trendError = trendError/(trainingData.size() - setupList.get(iSetup)[2]);
			
			vError.add(valueError);
			tError.add(trendError);
		}
		
		result.add(vError);
		result.add(tError);
		
		return result;
	}
	
	public double randomWalkProbabilityBias(int startDate, int endDate, ArrayList<Double> data)
	{
		double[] probabilityList = {0, 0.1, 0.2, 0.25, 0.3, 0.35, 0.4, 0.45};
		
		int count = 0;
		
		for(int i = startDate; i < endDate; i++)
		{
			if(data.get(i) - data.get(i - 1) > 0)
			{
				count++;
			}
			
			if(data.get(i) - data.get(i - 1) < 0)
			{
				count--;
			}
		}
	
		if(count > 7 || count < -7)
		{
			count = 7;
		}
		
		return probabilityList[Math.abs(count)];
	}
	
	public boolean randomWalkIndicator(int startDate, int endDate, ArrayList<Double> data)
	{
		int count = 0;
		
		for(int i = startDate; i < endDate; i++)
		{
			if(data.get(i) - data.get(i - 1) > 0)
			{
				count++;
			}
			
			if(data.get(i) - data.get(i - 1) < 0)
			{
				count--;
			}
		}
	
		if(count > 3 || count < -3)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public static void main(String[] args)
	{
		ArrayList<Double> data = readFileToDouble("Data - VNINDEX.txt");
		
		SeriesParticleFilterEngine Engine = new SeriesParticleFilterEngine();
		
		ArrayList<int[]> setupList = new ArrayList<int[]>();
		
		int[] particle = {3, 5, 10, 20, 30};
		int[] loop = {30, 60, 100, 300, 600, 1000};
		int[] day = {4, 6, 8};
		
		int[] quarter = {15, 27, 66, 101, 139, 177, 217, 259, 321, 385, 450, 505, 567, 631, 697, 754, 817, 881, 947, 1005, 1067, 1131, 1194, 1252, 1314, 1378, 
							1443, 1501, 1562, 1626, 1691, 1749, 1806, 1870, 1936, 1994, 2056, 2121, 2187, 2245, 2307, 2369, 2434, 2491, 2552, 2604};
		
		double vQuarterResult = 0;
		double tQuarterResult = 0;
		double vRQuarterResult = 0;
		double tRQuarterResult = 0;
		int qIndex = 1;
		
		Random setupIndex = new Random();
		
		for(int iParicle = 0; iParicle < particle.length; iParicle++)
		{
			for(int iLoop = 0; iLoop < loop.length; iLoop++)
			{
				for(int iDay = 0; iDay < day.length; iDay++)
				{
					int[] setup = new int[3];
					setup[0] = particle[iParicle];
					setup[1] = loop[iLoop];
					setup[2] = day[iDay];
					
					setupList.add(setup);
				}	
			}
		}
		
		int[] vSetupDist = new int[setupList.size()];
		int[] tSetupDist = new int[setupList.size()];
		
		for(int i = 0; i < vSetupDist.length; i++)
		{
			vSetupDist[i] = 0;
		}
		
		for(int i = 0; i < tSetupDist.length; i++)
		{
			tSetupDist[i] = 0;
		}
		
		try
		{
			BufferedWriter resultWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("result.txt"), "UTF-8"));
			BufferedWriter randSetResultWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("randSetResult.txt"), "UTF-8"));
			BufferedWriter logWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("statistic.txt"), "UTF-8"));
			BufferedWriter setupDistWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("setupDist.txt"), "UTF-8"));
			BufferedWriter quarterResultWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("quarterResult.txt"), "UTF-8"));
			BufferedWriter randomQuarterWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("randomQuarterResult.txt"), "UTF-8"));
			BufferedWriter rLogWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("rStatistic.txt"), "UTF-8"));
			
			ArrayList<Double> subdata = new ArrayList<Double>();
			subdata.addAll(data.subList(0, 14));
			ArrayList<ArrayList<Double>> result = Engine.parameterTraining(subdata, setupList);
			
			int vSetupNum = result.get(0).indexOf(Collections.min(result.get(0)));
			int tSetupNum = result.get(1).indexOf(Collections.min(result.get(1)));
			
			int[] vSetup = setupList.get(vSetupNum);
			int[] tSetup = setupList.get(tSetupNum);
			
			vSetupDist[vSetupNum]++;
			tSetupDist[tSetupNum]++;
			
			int vRandSetting = setupIndex.nextInt(90);
			int tRandSetting = setupIndex.nextInt(90);
			
			int[] vRSetup = setupList.get(vRandSetting);
			int[] tRSetup = setupList.get(tRandSetting);
			
			double vPredictValue = Engine.predictValue(14 - vSetup[2] + 1, 14, vSetup[0], vSetup[1], 1, data);
			double tPredictValue = Engine.predictValue(14 - tSetup[2] + 1, 14, tSetup[0], tSetup[1], 1, data);
			double vRPredictValue = Engine.predictValue(14 - vRSetup[2] + 1, 14, vRSetup[0], vRSetup[1], 1, data);
			double tRPredictValue = Engine.predictValue(14 - tRSetup[2] + 1, 14, tRSetup[0], tRSetup[1], 1, data);
			
			double tPredictValuePrev = tPredictValue;
			double tRPredictValuePrev = tRPredictValue;
			
			for(int i = 15; i < data.size(); i++)
			{	
				subdata = new ArrayList<Double>();
				subdata.addAll(data.subList(i - 15, i));
				
				System.out.println("Predict day: " + (i + 1));
				
				result = Engine.parameterTraining(subdata, setupList);
				
				vSetupNum = result.get(0).indexOf(Collections.min(result.get(0)));
				tSetupNum = result.get(1).indexOf(Collections.min(result.get(1)));
				
				vSetup = setupList.get(vSetupNum);
				tSetup = setupList.get(tSetupNum);
				
				vSetupDist[vSetupNum]++;
				tSetupDist[tSetupNum]++;
				
				vRandSetting = setupIndex.nextInt(90);
				tRandSetting = setupIndex.nextInt(90);
				
				vRSetup = setupList.get(vRandSetting);
				tRSetup = setupList.get(tRandSetting);
				
				logWriter.write("To be predicted day: " + (i + 1) + "\n");
				logWriter.write(result.get(0) + "\n");
				logWriter.write("min vValue: " + Collections.min(result.get(0)) + "\n");
				logWriter.write("min vSetting: " + vSetup[0] + " - " + vSetup[1] + " - " + vSetup[2] + "\n");
				logWriter.write(result.get(1) + "\n");
				logWriter.write("min tValue: " + Collections.min(result.get(1)) + "\n");
				logWriter.write("min tSetting: " + tSetup[0] + " - " + tSetup[1] + " - " + tSetup[2] + "\n");
				logWriter.write("\n");
				
				rLogWriter.write((i + 1) + "\t" + vRSetup[0] + " - " + vRSetup[1] + " - " + vRSetup[2]
				                         + "\t" + tRSetup[0] + " - " + tRSetup[1] + " - " + tRSetup[2] + "\n");
				
				vPredictValue = Engine.predictValue(i - vSetup[2] + 1, i, vSetup[0], vSetup[1], 1, data);
				tPredictValue = Engine.predictValue(i - tSetup[2] + 1, i, tSetup[0], tSetup[1], 1, data);
				vRPredictValue = Engine.predictValue(i - vRSetup[2] + 1, i, vRSetup[0], vRSetup[1], 1, data);
				tRPredictValue = Engine.predictValue(i - tRSetup[2] + 1, i, tRSetup[0], tRSetup[1], 1, data);
				
				resultWriter.write(vPredictValue + "\t" + tPredictValue + "\n");
				randSetResultWriter.write(vRPredictValue + "\t" + tRPredictValue + "\n");
				
				vQuarterResult += Math.abs(vPredictValue - data.get(i));
				vRQuarterResult += Math.abs(vRPredictValue - data.get(i));
				
				if((tPredictValue - tPredictValuePrev) * (data.get(i) - data.get(i - 1)) > 0)
				{
					tQuarterResult++;
				}
				
				if((tRPredictValue - tRPredictValuePrev) * (data.get(i) - data.get(i - 1)) > 0)
				{
					tRQuarterResult++;
				}
				
				tPredictValuePrev = tPredictValue;
				tRPredictValuePrev = tRPredictValue;
				
				if((i + 1) == quarter[qIndex])
				{
					quarterResultWriter.write((vQuarterResult/(quarter[qIndex] - quarter[qIndex - 1]))
												+ "\t" + tQuarterResult + "/" + (quarter[qIndex] - quarter[qIndex - 1])
												+ "\t" + (tQuarterResult/(quarter[qIndex] - quarter[qIndex - 1])) + "\n");
					
					randomQuarterWriter.write((vRQuarterResult/(quarter[qIndex] - quarter[qIndex - 1])) 
												+ "\t" + (tRQuarterResult/(quarter[qIndex] - quarter[qIndex - 1])) + "\n");
					
					qIndex++;
					
					vQuarterResult = 0;
					tQuarterResult = 0;
					
					vRQuarterResult = 0;
					tRQuarterResult = 0;
				}
				
			}
			
			for(int i = 0; i < vSetupDist.length; i++)
			{
				setupDistWriter.write(setupList.get(i)[0] + " - " + setupList.get(i)[1] + " - " + setupList.get(i)[2] + ":" + "\t" 
										+ vSetupDist[i] + "\t" +
										+ tSetupDist[i] + "\n");
			}
			
			setupDistWriter.close();
			logWriter.close();
			resultWriter.close();
			randSetResultWriter.close();
			quarterResultWriter.close();
			randomQuarterWriter.close();
			rLogWriter.close();
		}
		catch (UnsupportedEncodingException e) 
		{
			e.printStackTrace();
		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}

		/*
		for(int i = 10; i < data.size(); i++)
		{
			System.out.println(Engine.predictValue(i - 9, i, 1, data));
		}
		*/
		
		/*
		try 
		{	
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("1-10-100.txt"), "UTF-8"));
			
			for(int i = 1; i < data.size(); i++)
			{
				double[] result = Engine.functionGenerationThresholdTesting(data.get(i - 1), data.get(i), 10, 1, 0.5);
				writer.write(result[0] + "\t" + result[1] + "\n");
			}
			
			writer.close();
		} 
		catch (UnsupportedEncodingException e) 
		{
			e.printStackTrace();
		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		*/
		
		
		/*
		int numPar[] = {10, 20, 30, 40, 50, 100, 130, 160, 200};
		int numLoop[] = {10, 30, 60, 90, 100};
		
		try 
		{	
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("Statistic.txt"), "UTF-8"));
			
			
			writer.write("Particle \t Loop \t Mean \t STDDEV \t Max \t Min \n");
			
			for(int iLoop = 0; iLoop < numLoop.length; iLoop++)
			{
				for(int iPar = 0; iPar < numPar.length; iPar++)
				{
					ArrayList<Double> errorList = new ArrayList<Double>();
					
					for(int i = 1; i < data.size(); i++)
					{
						//double distance = Math.abs(data.get(i - 1) - data.get(i));
						double error = Engine.functionGenerationStatistic(data.get(i - 1), data.get(i), numPar[iPar], numLoop[iLoop], 0.5);
						//double percentage = error / data.get(i);
						
						errorList.add(error);
						
					}
					
					double mean = meanOfList(errorList);
					double stddev = stddevOfList(errorList);
					double min = Collections.min(errorList);
					double max = Collections.max(errorList);
					
					writer.write(numPar[iPar] + "\t" + numLoop[iLoop] + "\t" + mean + "\t" + stddev + "\t" + max + "\t" + min + "\n");
				}
			}

			
			writer.close();
		} 
		catch (UnsupportedEncodingException e) 
		{
			e.printStackTrace();
		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
		*/
	}
}
