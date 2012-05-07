package org.hcmus.stock.particlefilter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
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
			
			if(functionError < (threshold * referenceValue))
				return distribution;
			
			distributionList.add(distribution);
			errorList.add(functionError);
		}
		
		return distributionList.get(errorList.indexOf(Collections.min(errorList)));
	}

	public double predictValue(int startDate, int endDate, int numParticle, int numLoop, double threshold, double radius,  ArrayList<Double> data)
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
		
		ArrayList<Double> weightList = normalizer(weighting(distribution, radius));
		
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
	
	public void predictValueDemo(int startDate, int endDate, int numParticle, int numLoop, double threshold, double radius,  ArrayList<Double> data)
	{
		try
		{
			BufferedWriter demoWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("dayStimulate.txt")));
			BufferedWriter pAndw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("particleAndWeight.txt")));
			
			double result = 0;
			
			ArrayList<Double> distribution = new ArrayList<Double>();
			
			int count = 1;
			
			for(int i = startDate; i < endDate; i++)
			{
				ArrayList<Double> distributionElement = functionGeneration(data.get(i - 1), data.get(i), numParticle, numLoop, threshold, 0.5);
				
				for(int j = 0; j < distributionElement.size(); j++)
				{
					double value = (data.get(i - 1) * 0.05 * distributionElement.get(j)) + data.get(i - 1);
					demoWriter.write(count + "\t" + value + "\t" + count + "\t" + distributionElement.get(j) + "\n");
				}
				
				count++;
	
				distribution.addAll(distributionElement);
			}
			
			ArrayList<Double> weightList = weighting(distribution, radius);
			ArrayList<Double> weightListNorm = normalizer(weightList);
			
			for(int index = 0; index < distribution.size(); index++)
			{
				pAndw.write(distribution.get(index) + "\t" + weightList.get(index) + "\t" + weightListNorm.get(index) + "\n");
			}
			
			double distance = 0;
			
			for(int i = 0; i < distribution.size(); i++)
			{
				distance += data.get(endDate - 1) * 0.05 * distribution.get(i) * weightListNorm.get(i);
			}
			
			System.out.println(distance);
			
			result = data.get(endDate - 1) + distance;
		
			demoWriter.write(count + "\t" + data.get(endDate) + "\t" + count + "\t" + data.get(endDate) + "\n");
			demoWriter.write(count + "\t" + result + "\t" + count + "\t" + result);
			
			demoWriter.close();
			pAndw.close();
		}
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
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

	public ArrayList<ArrayList<Double>> parameterTraining(ArrayList<Double> trainingData, ArrayList<int[]> setupList, double radius)
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
				double predictedValue = predictValue(i - (setupList.get(iSetup)[2] - 1), i, setupList.get(iSetup)[0], setupList.get(iSetup)[1], 1, radius, trainingData);
				
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
	
	public static void parameterStatistics()
	{
		SeriesParticleFilterEngine engine = new SeriesParticleFilterEngine();
		
		int[] particle = {3, 5, 10, 20, 30, 100};
		int[] loop = {30, 60, 100, 300, 600, 1000};
		int[] day = {4, 6, 8, 10, 15};
		double[] threshold = {0.01, 0.03, 0.05, 0.1};
		double[] radius = {0.01, 0.03, 0.1, 0.3, 1};
		
		int[] quarter = {15, 27, 66, 101, 139, 177, 217, 259, 321, 385, 450, 505, 567, 631, 697, 754, 817, 881, 947, 1005, 1067, 1131, 1194, 1252, 1314, 1378, 
				1443, 1501, 1562, 1626, 1691, 1749, 1806, 1870, 1936, 1994, 2056, 2121, 2187, 2245, 2307, 2369, 2434, 2491, 2552, 2604};
		
		class Setting
		{
			int particle;
			int loop;
			int day;
			double threshold;
			double radius;
		}
		
		ArrayList<Setting> setting = new ArrayList<Setting>();
		ArrayList<Double> tResultList = new ArrayList<Double>();
		ArrayList<Double> vResultList = new ArrayList<Double>();
		
		for(int i = 0; i < particle.length; i++)
		{
			for(int j = 0; j < loop.length; j++)
			{
				for(int k = 0; k < day.length; k++)
				{
					for(int m = 0; m < threshold.length; m++)
					{
						for(int n = 0; n < radius.length; n++)
						{
							Setting temp = new Setting();
							
							temp.particle = particle[i];
							temp.loop = loop[j];
							temp.day = day[k];
							temp.threshold = threshold[m];
							temp.radius = radius[n];
							
							setting.add(temp);
						}
					}
				}
			}
		}
		
		ArrayList<Double> data = readFileToDouble("Data - VNINDEX.txt");
		
		for(int counting = 11; counting < 20; counting++)
		{
			try
			{
				(new File((counting + 1) + "th")).mkdirs();
				
				BufferedWriter settingResults = new BufferedWriter(new OutputStreamWriter(new FileOutputStream((counting + 1) + "th\\settingResults.txt")));
				BufferedWriter settingStatistics = new BufferedWriter(new OutputStreamWriter(new FileOutputStream((counting + 1) + "th\\settingStatistics.txt")));
				
				for(int s = 0; s < setting.size(); s++)
				{	
					
					System.out.println(setting.get(s).particle + " - " + setting.get(s).day + " - " + setting.get(s).loop + " - " + setting.get(s).threshold + " - " + setting.get(s).radius);
					
					double predictValue = engine.predictValue(15 - setting.get(s).day + 1, 15, setting.get(s).particle, setting.get(s).loop, setting.get(s).threshold, setting.get(s).radius, data);
					double previousPredictValue = predictValue;
					
					double tResult = 0;
					double vResult = 0;
					
					double count = 0;
					
					for(int i = 16; i < 619; i++)
					{
						 predictValue = engine.predictValue(i - setting.get(s).day + 1, i, setting.get(s).particle, setting.get(s).loop, setting.get(s).threshold, setting.get(s).radius, data);
						
						 vResult += Math.abs(predictValue - data.get(i));
						 
						 if((predictValue - previousPredictValue) * (data.get(i) - data.get(i - 1)) > 0)
						 {
							 tResult++;
						 }
						 
						 previousPredictValue = predictValue;
						 
						 count++;
					}
					
					settingResults.write(setting.get(s).particle + "\t" 
											+ setting.get(s).day + "\t" 
											+ setting.get(s).loop + "\t" 
											+ setting.get(s).threshold + "\t" 
											+ setting.get(s).radius + "\t"
											+ (tResult/count) + "\t"
											+ (vResult/count) + "\n");
					
					tResultList.add(tResult);
					vResultList.add(vResult);
				}
				
				
				int[] pTCounter = new int[6];
				int[] pVCounter = new int[6];
				for(int i = 0; i < pTCounter.length; i++)
				{
					pTCounter[i] = 0;
					pVCounter[i] = 0;
				}
				
				int[] lTCounter = new int[6];
				int[] lVCounter = new int[6];
				for(int i = 0; i < lTCounter.length; i++)
				{
					lTCounter[i] = 0;
					lVCounter[i] = 0;
				}
				
				int[] dTCounter = new int[5];
				int[] dVCounter = new int[5];
				for(int i = 0; i < dTCounter.length; i++)
				{
					dTCounter[i] = 0;
					dVCounter[i] = 0;
				}
				
				int[] tTCounter = new int[4];
				int[] tVCounter = new int[4];
				for(int i = 0; i < tTCounter.length; i++)
				{
					tTCounter[i] = 0;
					tVCounter[i] = 0;
				}
				
				int[] rTCounter = new int[5];
				int[] rVCounter = new int[5];
				for(int i = 0; i < rTCounter.length; i++)
				{
					rTCounter[i] = 0;
					rVCounter[i] = 0;
				}
				
				for(int i = 0; i < 600; i++)
				{
					ArrayList<Double> tCompareList = new ArrayList<Double>();
					ArrayList<Double> vCompareList = new ArrayList<Double>();	
					
					for(int j = 0; j < 6; j++)
					{
						tCompareList.add(tResultList.get(i + j * 600));
						vCompareList.add(vResultList.get(i + j * 600));
					}
					
					pTCounter[tCompareList.indexOf(Collections.max(tCompareList))]++;
					pVCounter[vCompareList.indexOf(Collections.min(vCompareList))]++;
				}
				
				for(int i = 0; i < 600; i++)
				{
					ArrayList<Double> tCompareList = new ArrayList<Double>();
					ArrayList<Double> vCompareList = new ArrayList<Double>();
					
					for(int j = 0; j < 6; j++)
					{
						tCompareList.add(tResultList.get(i + j * 100));
						vCompareList.add(vResultList.get(i + j * 100));
					}
					
					lTCounter[tCompareList.indexOf(Collections.max(tCompareList))]++;
					lVCounter[vCompareList.indexOf(Collections.min(vCompareList))]++;
				}
				
				for(int i = 0; i < 720; i++)
				{
					ArrayList<Double> tCompareList = new ArrayList<Double>();
					ArrayList<Double> vCompareList = new ArrayList<Double>();
					
					for(int j = 0; j < 5; j++)
					{
						tCompareList.add(tResultList.get(i + j * 20));
						vCompareList.add(vResultList.get(i + j * 20));
					}
					
					dTCounter[tCompareList.indexOf(Collections.max(tCompareList))]++;
					dVCounter[vCompareList.indexOf(Collections.min(vCompareList))]++;
				}
				
				for(int i = 0; i < 900; i++)
				{
					ArrayList<Double> tCompareList = new ArrayList<Double>();
					ArrayList<Double> vCompareList = new ArrayList<Double>();
					
					for(int j = 0; j < 4; j++)
					{
						tCompareList.add(tResultList.get(i + j * 5));
						vCompareList.add(vResultList.get(i + j * 5));
					}
					
					tTCounter[tCompareList.indexOf(Collections.max(tCompareList))]++;
					tVCounter[vCompareList.indexOf(Collections.min(vCompareList))]++;
				}
				
				for(int i = 0; i < 720; i++)
				{
					ArrayList<Double> tCompareList = new ArrayList<Double>();
					ArrayList<Double> vCompareList = new ArrayList<Double>();
					
					for(int j = 0; j < 4; j++)
					{
						tCompareList.add(tResultList.get(i + j * 5));
						vCompareList.add(vResultList.get(i + j * 5));
					}
					
					rTCounter[tCompareList.indexOf(Collections.max(tCompareList))]++;
					rVCounter[vCompareList.indexOf(Collections.min(vCompareList))]++;
				}
				
				settingStatistics.write("Particle:\n");
				settingStatistics.write("Trending:\n");
				for(int i = 0; i < particle.length; i++)
				{
					settingStatistics.write(particle[i] + ": " + pTCounter[i] + "\n");
				}
				settingStatistics.write("Value:\n");
				for(int i = 0; i < particle.length; i++)
				{
					settingStatistics.write(particle[i] + ": " + pVCounter[i] + "\n");
				}
				settingStatistics.write("\n");
				
				settingStatistics.write("Loop:\n");
				settingStatistics.write("Trending:\n");
				for(int i = 0; i < loop.length; i++)
				{
					settingStatistics.write(loop[i] + ": " + lTCounter[i] + "\n");
				}
				settingStatistics.write("Value:\n");
				for(int i = 0; i < loop.length; i++)
				{
					settingStatistics.write(loop[i] + ": " + lVCounter[i] + "\n");
				}
				settingStatistics.write("\n");
				
				settingStatistics.write("Day:\n");
				settingStatistics.write("Trending:\n");
				for(int i = 0; i < day.length; i++)
				{
					settingStatistics.write(day[i] + ": " + dTCounter[i] + "\n");
				}
				settingStatistics.write("Value:\n");
				for(int i = 0; i < day.length; i++)
				{
					settingStatistics.write(day[i] + ": " + dVCounter[i] + "\n");
				}
				settingStatistics.write("\n");
				
				settingStatistics.write("Threshold:\n");
				settingStatistics.write("Trending:\n");
				for(int i = 0; i < threshold.length; i++)
				{
					settingStatistics.write(threshold[i] + ": " + tTCounter[i] + "\n");
				}
				settingStatistics.write("Value:\n");
				for(int i = 0; i < threshold.length; i++)
				{
					settingStatistics.write(threshold[i] + ": " + tVCounter[i] + "\n");
				}
				settingStatistics.write("\n");
				
				settingStatistics.write("Radius:\n");
				settingStatistics.write("Trending:\n");
				for(int i = 0; i < radius.length; i++)
				{
					settingStatistics.write(radius[i] + ": " + rTCounter[i] + "\n");
				}
				settingStatistics.write("Value:\n");
				for(int i = 0; i < radius.length; i++)
				{
					settingStatistics.write(radius[i] + ": " + rVCounter[i] + "\n");
				}
				settingStatistics.write("\n");
				
				settingStatistics.close();
				settingResults.close();
			}
			catch (FileNotFoundException e) 
			{
				e.printStackTrace();
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
	}
	
	public static void parameterChooser()
	{
		SeriesParticleFilterEngine engine = new SeriesParticleFilterEngine();
		
		int[] particle = {3, 10, 30};
		int[] day = {4, 6, 8};
		double[] radius = {0.01, 0.03, 0.1};
		
		class Setting
		{
			int particle;
			int day;
			double radius;
			ArrayList<Double> tValue; 
			ArrayList<Double> vError;
			
			Setting()
			{
				tValue = new ArrayList<Double>();
				vError = new ArrayList<Double>();
			}
			
			Setting(int particle, int day, double radius)
			{
				this.particle = particle;
				this.day = day;
				this.radius = radius;
				
				tValue = new ArrayList<Double>();
				vError = new ArrayList<Double>();
			}
			
			public double avgTValue()
			{
				double sum = 0;
				for(int i = 0; i < tValue.size(); i++)
				{
					sum += tValue.get(i);
				}
				
				return sum/(tValue.size() * 1.0);
			}
			
			public double avgVError()
			{
				double sum = 0;
				for(int i = 0; i < vError.size(); i++)
				{
					sum += vError.get(i);
				}
				
				return sum/(vError.size() * 1.0);
			}
		}
		
		ArrayList<Setting> settingList = new ArrayList<Setting>();
		
		for(int i = 0; i < particle.length; i++)
		{
			for(int j = 0; j < day.length; j++)
			{
				for(int k = 0; k < radius.length; k++)
				{
					Setting newSetting = new Setting(particle[i], day[j], radius[k]);
					
					settingList.add(newSetting);
				}
			}
		}
		
		ArrayList<Double> data = readFileToDouble("Data - VNINDEX.txt");
		
		for(int times = 0; times < 5; times++)
		{
			System.out.println("Runing times: " + (times + 1));
			for(int i = 0; i < settingList.size(); i++)
			{
				System.out.println(
						settingList.get(i).particle + " - " +
						settingList.get(i).day + " - " + 
						settingList.get(i).radius);
				
				double predictValue = engine.predictValue(8 - settingList.get(i).day + 1, 8, settingList.get(i).particle, 1000, 0.01, settingList.get(i).radius, data);
				double previousPredictValue = predictValue;
				
				double tValue = 0;
				double vError = 0;
				
				double count = 0;
				
				for(int date = 9; date < 619; date++)
				{
					predictValue = engine.predictValue(date - settingList.get(i).day + 1, date, settingList.get(i).particle, 1000, 0.01, settingList.get(i).radius, data);
					
					vError += Math.abs(predictValue - data.get(date));
					
					if((predictValue - previousPredictValue) * (data.get(date) - data.get(date - 1)) > 0)
					{
						tValue++;
					}
					
					previousPredictValue = predictValue;
					count++;
				}
				
				settingList.get(i).tValue.add(tValue/count);
				settingList.get(i).vError.add(vError/count);
			}
		}
		
		try 
		{
			BufferedWriter settingStatistics = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("settingStatistics.txt")));
			
			settingStatistics.write("id" + "\t" + "particle" + "\t" + "day" + "\t" + "radius" + "\t" + "avgTValue" + "\t" + "avgVError" + "\n");
			
			for(int i = 0; i < settingList.size(); i++)
			{
				settingStatistics.write((i + 1) + 
						"\t" + settingList.get(i).particle + 
						"\t" + settingList.get(i).day + 
						"\t" + settingList.get(i).radius +
						"\t" + settingList.get(i).avgTValue() + 
						"\t" + settingList.get(i).avgVError() + "\n");
			}
			
			settingStatistics.close();
		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}

		
	}
	
	public static void main(String[] args)
	{
		parameterChooser();
	}
}
