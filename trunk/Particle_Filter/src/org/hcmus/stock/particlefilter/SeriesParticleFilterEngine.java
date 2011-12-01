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

import javax.sql.rowset.Predicate;

public class SeriesParticleFilterEngine 
{
	private ArrayList<Double> referenceData;
	private int numFunction;
	private double threshold;
	private int startDate;
	private int endDate;

	public SeriesParticleFilterEngine()
	{}
	
	public SeriesParticleFilterEngine(ArrayList<Double> inputReferenceData, 
										int inputStartDate, 
										int inputEndDate, 
										int inputNumParticle, 
										double inputThresHold)
	{
		referenceData = inputReferenceData;
		startDate = inputStartDate;
		endDate = inputEndDate;
		numFunction = inputNumParticle;
		threshold = inputThresHold;
	}
	
	public SeriesParticleFilterEngine(ArrayList<Double> inputReferenceData)
	{
		referenceData = inputReferenceData;
	}
	
	public ArrayList<Double> functionGeneration(double anchorValue, double referenceValue, int numParticle, int numLoop, double threshold, double riseAndFallIndicator)
	{
		ArrayList<ArrayList<Double>> distributionList = new ArrayList<ArrayList<Double>>();
		
		ArrayList<Double> errorList = new ArrayList<Double>();
		
		for(int loop = 0; loop < numLoop; loop++)
		{
			double functionError = 0;
			ArrayList<Double> distribution = new ArrayList<Double>();
			ArrayList<Double> priceList = new ArrayList<Double>();
			
			for(int i = 0; i < numParticle; i++)
			{
				Random generator = new Random();
				
				double differences = 0;
				
				double probability = generator.nextDouble();
				
				if(probability > riseAndFallIndicator)
				{
					probability = generator.nextDouble();
					
					distribution.add(probability);
					
					differences = anchorValue * 0.05 * probability;
				}
				else
				{
					probability = - generator.nextDouble();
					
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

	public double predictedValue(int startDate, int endDate, double threshold, ArrayList<Double> Data)
	{
		double result = 0;
		
		ArrayList<Double> distribution = new ArrayList<Double>();
	
		//Actually the initial i must take value startDate - 1, but due to the fact that we need
		//a pair of value for the functionGeneration module to work then we shift the initially to 1
		//We can see in the anchorValue of the functionGeneration module we use Data.get(i - 1), that is the actual startDate
		for(int i = startDate; i < endDate; i++)
		{
			ArrayList<Double> distributionElement = functionGeneration(Data.get(i - 1), Data.get(i), 10, 1000, threshold, 0.5);
			
			distribution.addAll(distributionElement);
		}
		
		ArrayList<Double> weightList = normalizer(weighting(distribution, 0.15));
		
		double distance = 0;
		
		for(int i = 0; i < distribution.size(); i++)
		{
			distance += Data.get(endDate - 1) * 0.05 * distribution.get(i) * weightList.get(i);
		}
		
		result = Data.get(endDate - 1) + distance;
		
		//System.out.println(Data.get(endDate - 1) + "\t" + distance + "\t" + result);
		
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
	
	public void setReferenceData(ArrayList<Double> referenceData) {
		this.referenceData = referenceData;
	}
	
	public ArrayList<Double> getReferenceData() {
		return referenceData;
	}

	public void setNumParticle(int numParticle) {
		this.numFunction = numParticle;
	}

	public int getNumParticle() {
		return numFunction;
	}

	public void setThreshold(double threshold) {
		this.threshold = threshold;
	}

	public double getThreshold() {
		return threshold;
	}

	public void setStartDate(int startDate) {
		this.startDate = startDate;
	}

	public int getStartDate() {
		return startDate;
	}

	public void setEndDate(int endDate) {
		this.endDate = endDate;
	}

	public int getEndDate() {
		return endDate;
	}
	
	public static void main(String[] args)
	{
		ArrayList<Double> data = readFileToDouble("Data - VNINDEX.txt");
		
		SeriesParticleFilterEngine Engine = new SeriesParticleFilterEngine();
		
		for(int i = 10; i < data.size(); i++)
		{
			System.out.println(Engine.predictedValue(i - 9, i, 1, data));
		}
		
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
