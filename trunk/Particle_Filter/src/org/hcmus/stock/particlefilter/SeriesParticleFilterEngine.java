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
	private ArrayList<Double> referenceData;
	private int numParticle;
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
		numParticle = inputNumParticle;
		threshold = inputThresHold;
	}
	
	public SeriesParticleFilterEngine(ArrayList<Double> inputReferenceData)
	{
		referenceData = inputReferenceData;
	}
	
	public ArrayList<Double> functionGeneration(double anchorValue, double referenceValue, int numParticle, double threshold)
	{
		ArrayList<Double> result = new ArrayList<Double>();
	
		ArrayList<Double> errorList = new ArrayList<Double>();
		
		for(int loop = 0; loop < 100; loop++)
		{
			double functionError = 0;
			
			for(int i = 0; i < numParticle; i++)
			{
				Random generator = new Random();
				
				double differences = 0;
				
				double probability = generator.nextDouble();
				
				if(probability > 0.5)
				{
					differences = anchorValue * 0.05 * probability;
				}
				else
				{
					differences = - (anchorValue * 0.05 * (1 - probability));
				}
				
				result.add(anchorValue + differences);
			}
			
			for(int i = 0; i < numParticle; i++)
			{
				functionError += Math.abs(result.get(i) - referenceValue);
			}
			
			functionError = functionError / numParticle;
			
			/*
			if(functionError < threshold)
				break;
			*/
			
			errorList.add(functionError);
		}
		
		double errorMin = Collections.min(errorList);

		System.out.println(errorMin);
		
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
		this.numParticle = numParticle;
	}

	public int getNumParticle() {
		return numParticle;
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
		
	}
}
