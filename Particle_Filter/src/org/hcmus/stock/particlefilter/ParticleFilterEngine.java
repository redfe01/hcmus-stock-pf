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

public class ParticleFilterEngine 
{
	private ArrayList<Double> particle;
	private ArrayList<Double> weight;
	private ArrayList<Double> data;
	
	ParticleFilterEngine()
	{
		particle = new ArrayList<Double>();
		weight = new ArrayList<Double>();
		data = new ArrayList<Double>();
	}
	
	ParticleFilterEngine(int numParticle, ArrayList<Double> inputData)
	{
		particle = new ArrayList<Double>();
		weight = new ArrayList<Double>();
		data = inputData;
		
		for(int i = 0; i < numParticle; i++)
		{
			particle.add(data.get(0));
			
			double initWeight = 1.0 / (numParticle * 1.0);
			
			weight.add(initWeight);
		}
	}

	private void sense(double priceValue)
	{
		for(int i = 0; i < particle.size(); i++)
		{
			weight.set(i, 1 / Math.abs(particle.get(i) - priceValue));
		}
		
		normalize();
	}
	
	private void resampling()
	{
		Random generator = new Random();
		
		double max = Collections.max(weight);
	
		ArrayList<Double> newParticle = new ArrayList<Double>();
		
		int index = generator.nextInt(weight.size());
		double beta = 0;
		for(int i = 0; i < weight.size(); i++)
		{
			beta = beta + generator.nextDouble() * max * 2;
			while(weight.get(index) < beta)
			{
				beta = beta - weight.get(i);
				index = (index + 1) % weight.size();
			}
			newParticle.add(particle.get(index));
		}
		particle = newParticle;
	}
	
	private void move()
	{
		Random primary = new Random();
		
		for(int i = 0; i < particle.size(); i++)
		{
			Random riseOrFall = new Random();
			double indicator = riseOrFall.nextDouble();
			double newPValue;
			
			if(indicator > 0.5)
			{
				newPValue = particle.get(i) + (particle.get(i) * primary.nextDouble() * 0.05);
			}
			else
			{
				newPValue = particle.get(i) - (particle.get(i) * primary.nextDouble() * 0.05);
			}
			
			particle.set(i, newPValue);
		}
	}
	
	private void normalize()
	{
		double sum = 0.0;
		for(int i = 0; i < weight.size(); i++)
		{
			sum += weight.get(i);
		}
		
		for(int i = 0; i < weight.size(); i++)
		{
			weight.set(i, weight.get(i) / sum);
		}
	}
	
	public double predict()
	{
		double result = 0;
		
		ArrayList<Double> sumOfParticle = new ArrayList<Double>();
		ArrayList<Double> sumOfWeight = new ArrayList<Double>(); 
		
		for(int i = 0; i < data.size(); i++)
		{
			move();
			sense(data.get(i));
			
			sumOfParticle.addAll(particle);
			sumOfWeight.addAll(weight);
			
			resampling();
		}
		
		/*
		normalize();
		
		for(int i = 0; i < particle.size(); i++)
		{
			result += particle.get(i) * weight.get(i);
		}
		*/
		
		for(int i = 0; i < sumOfParticle.size(); i++)
		{
			result += sumOfParticle.get(i) * sumOfWeight.get(i);
		}
		
		return result/5.0;
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
	
	public static void main(String args[])
	{
		ArrayList<Double> data = readFileToDouble("Data - VNINDEX.txt");
		
		ArrayList<Double> subdata;
		
		int[] quarter = {6, 27, 66, 101, 139, 177, 217, 259, 321, 385, 450, 505, 567, 631, 697, 754, 817, 881, 947, 1005, 1067, 1131, 1194, 1252, 1314, 1378, 
				1443, 1501, 1562, 1626, 1691, 1749, 1806, 1870, 1936, 1994, 2056, 2121, 2187, 2245, 2307, 2369, 2434, 2491, 2552, 2604};

		double vQuarterResult = 0;
		double tQuarterResult = 0;
		
		double vQuarterDataSum = 0;
		double vQuarterPersentResult = 0;
		
		int qIndex = 1;
		double predictValue = 0;
		 
		subdata = new ArrayList<Double>();
		subdata.addAll(data.subList(0, 5));
		ParticleFilterEngine engine = new ParticleFilterEngine(100, subdata);
		predictValue = engine.predict();
		
		double predictValuePrev = predictValue;
		
		try
		{
			BufferedWriter resultWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("result.txt"), "UTF-8"));
			//BufferedWriter logWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("statistic.txt"), "UTF-8"));
			BufferedWriter quarterResultWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("quarterResult.txt"), "UTF-8"));
			
			
			for(int i = 6; i < data.size(); i++)
			{
				subdata = new ArrayList<Double>();
				subdata.addAll(data.subList(i - 5, i));
				
				engine = new ParticleFilterEngine(100, subdata);
				
				predictValue = engine.predict();
				
				System.out.println("Predict day: " + (i + 1));
				
				resultWriter.write(predictValue + "\n");
				
				vQuarterResult += Math.abs(predictValue - data.get(i));
				
				vQuarterDataSum += data.get(i);
				
				if((predictValue - predictValuePrev) * (data.get(i) - data.get(i - 1)) > 0)
				{
					tQuarterResult++;
				}
				
				predictValuePrev = predictValue;
				
				if((i + 1) == quarter[qIndex])
				{
					double dayRange = quarter[qIndex] - quarter[qIndex - 1];
					
					vQuarterPersentResult = (vQuarterResult / dayRange) / (vQuarterDataSum / dayRange);
					
					quarterResultWriter.write((vQuarterResult / dayRange) 
												+ "\t" + (vQuarterDataSum / dayRange)
												+ "\t" + vQuarterPersentResult
												+ "\t" + tQuarterResult + "/" + dayRange
												+ "\t" + (tQuarterResult/dayRange) + "\n");
					qIndex++;
					
					vQuarterResult = 0;
					tQuarterResult = 0;
					vQuarterDataSum = 0;
				}
			}

			resultWriter.close();
			quarterResultWriter.close();

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
