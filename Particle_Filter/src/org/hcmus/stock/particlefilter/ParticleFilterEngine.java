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

public class ParticleFilterEngine 
{
	private ArrayList<Double> particle;
	private ArrayList<Double> weight;
	
	ParticleFilterEngine()
	{
		particle = new ArrayList<Double>();
		weight = new ArrayList<Double>();
	}
	
	private void init(int numParticle)
	{
		particle.clear();
		weight.clear();
		
		Random riseOrFall = new Random();
		
		for(int i = 0; i < numParticle; i++)
		{
			Random magnitude = new Random();
			
			if(riseOrFall.nextDouble() > 0.5)
			{
				particle.add(magnitude.nextDouble());
			}
			else
			{
				particle.add(-magnitude.nextDouble());
			}
			
			double initWeight = 1.0 / (numParticle * 1.0);
			
			weight.add(initWeight);
		}
	}
	
	private void sense(double referenceValue)
	{
		for(int i = 0; i < particle.size(); i++)
		{
			double newWeight = Math.exp(-Math.abs(particle.get(i) - referenceValue));
			
			newWeight = weight.get(i) * newWeight;
			
			weight.set(i, newWeight);
		}
		
		normalize();
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
	
	private void move()
	{
		Random riseOrFall = new Random();
		
		double noiseValue = 0;
		
		for(int i = 0; i < particle.size(); i++)
		{
			Random primary = new Random();
			
			if(riseOrFall.nextDouble() > 0.5)
			{
				noiseValue = (primary.nextDouble() / 10.0);
			}
			else
			{
				noiseValue = -(primary.nextDouble() / 10.0);
			}
			
			double newValue = particle.get(i) + noiseValue;
			
			if(newValue > 1.0)
			{
				newValue = 1.0;
			}
			
			if(newValue < -1.0)
			{
				newValue = -1.0;
			}
			
			particle.set(i, newValue);
		}
	}	
	
	private void resampling()
	{
		Random generator = new Random();
		
		double max = Collections.max(weight);
	
		ArrayList<Double> newParticle = new ArrayList<Double>();
		ArrayList<Double> newWeight = new ArrayList<Double>();
		
		int index = generator.nextInt(weight.size());
		double beta = 0;
		
		for(int i = 0; i < particle.size(); i++)
		{
			beta = beta + generator.nextDouble() * max * 2;

			while(weight.get(index) < beta)
			{
				beta = beta - weight.get(index);
				index = (index + 1) % weight.size();
			}
			newParticle.add(particle.get(index));
			newWeight.add(1.0 / (particle.size() * 1.0));
		}
		
		particle = newParticle;
		weight = newWeight;
		
		normalize();
	}
	
	private void resamplingTesting(BufferedWriter testing)
	{
		try
		{
			Random generator = new Random();
			
			double max = Collections.max(weight);
		
			ArrayList<Double> newParticle = new ArrayList<Double>();
			ArrayList<Double> newWeight = new ArrayList<Double>();
			
			int index = generator.nextInt(weight.size());
			double beta = 0;
			
			testing.write("Index: " + index + "\n");
			testing.write("Max: " + max + "\n");
			
			for(int i = 0; i < particle.size(); i++)
			{
				testing.write("Iteration: " + (i + 1) + "\n");
				
				beta = beta + generator.nextDouble() * max * 2;				
				
				testing.write("Beta: " + beta + "\n");
				testing.write("Weight-Index: " + weight.get(index) + "\n");
				
				int count = 1;
				
				while(weight.get(index) < beta)
				{
					testing.write("Loop: " + count + "\n");
					beta = beta - weight.get(index);
					index = (index + 1) % weight.size();
					
					testing.write("New Beta: " + beta + "\n");
					testing.write("New Index: " + index + "\n");
					testing.write("New Weight-Index: " + weight.get(index) + "\n");
					
					count++;
				}
				
				testing.write("\n");
				newParticle.add(particle.get(index));
				newWeight.add(weight.get(index));
			}
			
			particle = newParticle;
			weight = newWeight;
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public double predict(ArrayList<Double> data, int numParticle)
	{
		double result = 0.0;
		
		double referenceValue = 0;
		
		init(numParticle);
		
		for(int i = 1; i < data.size(); i++)
		{
			referenceValue = ((data.get(i) - data.get(i - 1))/data.get(i - 1))/0.05;
			
			sense(referenceValue);
			
			resampling();
			
			move();
		}
		
		double difference = 0;
		
		for(int i = 0; i < particle.size(); i++)
		{
			difference += particle.get(i) * weight.get(i);
		}
		
		result = (data.get((data.size() - 1)) * 0.05 * difference) + data.get((data.size() - 1));
		
		return result;
	}
	
	public double predictTesting(ArrayList<Double> data, int numParticle)
	{
		double result = 0.0;
		
		try
		{
			BufferedWriter testing = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("testingDistribution.txt")));
			
			double referenceValue = 0;
			
			init(numParticle);
			testing.write("Initial\n");
			for(int i = 0; i < particle.size(); i++)
			{
				testing.write(particle.get(i) + "\t" + weight.get(i) + "\n");
			}
			testing.write("\n");
			
			
			for(int i = 1; i < data.size(); i++)
			{
				testing.write("Iteration: " + i + "\n");
				
				referenceValue = ((data.get(i) - data.get(i - 1))/data.get(i - 1))/0.05;
				
				sense(referenceValue);
				testing.write("Sensing\n");
				for(int index = 0; index < particle.size(); index++)
				{
					testing.write(particle.get(index) + "\t" + weight.get(index) + "\n");
				}
				testing.write("\n");
				
				resampling();
				testing.write("Resampling\n");
				for(int index = 0; index < particle.size(); index++)
				{
					testing.write(particle.get(index) + "\t" + weight.get(index) + "\n");
				}
				testing.write("\n");
				
				move();
				testing.write("Move\n");
				for(int index = 0; index < particle.size(); index++)
				{
					testing.write(particle.get(index) + "\t" + weight.get(index) + "\n");
				}
				testing.write("\n");
			}
			
			testing.close();
			
			double difference = 0;
			
			for(int i = 0; i < particle.size(); i++)
			{
				difference += particle.get(i) * weight.get(i);
			}
			
			result = (data.get((data.size() - 1)) * 0.05 * difference) + data.get((data.size() - 1));
			
		}
		catch(FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		
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
	
	public static void testingResult(int maxTimes)
	{
		ParticleFilterEngine engine = new ParticleFilterEngine();
		
		int[] quarter = {684, 749, 808};
		
		ArrayList<Double> data = readFileToDouble("Data - VNINDEX.txt");
		
		ArrayList<Double> tValueList = new ArrayList<Double>();
		ArrayList<Double> vErrorList = new ArrayList<Double>();
		
		ArrayList<double[]> tValueQuarterList = new ArrayList<double[]>();
		ArrayList<double[]> vErrorQuarterList = new ArrayList<double[]>();
		
		(new File("Individual Result")).mkdirs();
		
		for(int times = 0; times < maxTimes; times++)
		{
			System.out.println(times + 1);
			
			ArrayList<Double> subdata = new ArrayList<Double>();
			subdata.addAll(data.subList(618 - 8, 618));
			
			double predictedValue = engine.predict(subdata, 100);
			double previousPredictedValue = predictedValue;
			
			double tValue = 0;
			double vError = 0;
			
			double[] tQuarterResult = {0, 0, 0};
			double[] vQuarterResult = {0, 0, 0};
			
			int quarterCounter = 0;
			int counter = 0;
			int quarterIndex = 0;
			
			try 
			{
				BufferedWriter result = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("Individual Result\\" + (times + 1) + ".txt")));
				
				for(int i = 619; i < data.size(); i++)
				{	
					subdata = new ArrayList<Double>();
					subdata.addAll(data.subList(i - 8, i));
					
					predictedValue = engine.predict(subdata, 100);
					
					result.write((i + 1) + "\t" + data.get(i) + "\t"  + predictedValue + "\n");
					
					vError += Math.abs(predictedValue - data.get(i));
					if(quarterIndex < 3)
					{
						vQuarterResult[quarterIndex] += Math.abs(predictedValue - data.get(i));
					}
					
					if((predictedValue - previousPredictedValue) * (data.get(i) - data.get(i - 1)) > 0)
					{
						tValue++;
						if(quarterIndex < 3)
						{
							tQuarterResult[quarterIndex]++;
						}
					}
					
					quarterCounter++;
					counter++;
					
					if(quarterIndex < 3)
					{
						if((i + 1) == quarter[quarterIndex])
						{	
							tQuarterResult[quarterIndex] = 1 - (tQuarterResult[quarterIndex]/quarterCounter);
							vQuarterResult[quarterIndex] = vQuarterResult[quarterIndex]/quarterCounter;
							quarterCounter = 0;
							quarterIndex++;
						}
					}
					
					previousPredictedValue = predictedValue;
				}
				
				tValue = 1 - (tValue/counter);
				vError = vError/counter;
				
				result.write("\n");
				result.write("Trending Quarter Value:\n");
				result.write(tQuarterResult[0] + "\t" + tQuarterResult[1] + "\t" + tQuarterResult[2] + "\n");
				result.write("Value Quarter Error:\n");
				result.write(vQuarterResult[0] + "\t" + vQuarterResult[1] + "\t" + vQuarterResult[2] + "\n");
				
				result.write("\n");
				result.write("Trending:\t" + tValue + "\n");
				result.write("MAE:\t" + vError + "\n");
				
				result.close();
			} 
			catch (FileNotFoundException e) 
			{
				e.printStackTrace();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
						
			tValueList.add(tValue);
			vErrorList.add(vError);
			
			tValueQuarterList.add(tQuarterResult);
			vErrorQuarterList.add(vQuarterResult);
		}
		
		double tResult = 0;
		double vResult = 0;
		double tStd = 0;
		double vStd = 0;

		double[] tQuarterResult = {0, 0, 0};
		double[] vQuarterResult = {0, 0, 0};
		double[] tQuarterStd = {0, 0, 0};
		double[] vQuarterStd = {0, 0, 0};
		
		for(int i = 0; i < maxTimes; i++)
		{
			tResult += tValueList.get(i);
			vResult += vErrorList.get(i);
			
			tQuarterResult[0] += tValueQuarterList.get(i)[0];
			tQuarterResult[1] += tValueQuarterList.get(i)[1];
			tQuarterResult[2] += tValueQuarterList.get(i)[2];
			
			vQuarterResult[0] += vErrorQuarterList.get(i)[0];
			vQuarterResult[1] += vErrorQuarterList.get(i)[1];
			vQuarterResult[2] += vErrorQuarterList.get(i)[2]; 
		}
		
		tQuarterResult[0] = tQuarterResult[0]/maxTimes;
		tQuarterResult[1] = tQuarterResult[1]/maxTimes;
		tQuarterResult[2] = tQuarterResult[2]/maxTimes;
		
		vQuarterResult[0] = vQuarterResult[0]/maxTimes;
		vQuarterResult[1] = vQuarterResult[1]/maxTimes;
		vQuarterResult[2] = vQuarterResult[2]/maxTimes;
		
		tResult = tResult/maxTimes;
		vResult = vResult/maxTimes;
		
		for(int i = 0; i < maxTimes; i++)
		{
			tStd += Math.pow((tValueList.get(i) - tResult), 2);
			vStd += Math.pow((vErrorList.get(i) - vResult), 2);
			
			tQuarterStd[0] += Math.pow((tValueQuarterList.get(i)[0] - tQuarterResult[0]), 2);
			tQuarterStd[1] += Math.pow((tValueQuarterList.get(i)[1] - tQuarterResult[1]), 2);
			tQuarterStd[2] += Math.pow((tValueQuarterList.get(i)[2] - tQuarterResult[2]), 2);
			
			vQuarterStd[0] += Math.pow((vErrorQuarterList.get(i)[0] - vQuarterResult[0]), 2);
			vQuarterStd[1] += Math.pow((vErrorQuarterList.get(i)[1] - vQuarterResult[1]), 2);
			vQuarterStd[2] += Math.pow((vErrorQuarterList.get(i)[2] - vQuarterResult[2]), 2);
		}
		
		tStd = Math.sqrt(tStd/(maxTimes - 1));
		vStd = Math.sqrt(vStd/(maxTimes - 1));
			
		tQuarterStd[0] = Math.sqrt(tQuarterStd[0]/(maxTimes - 1));
		tQuarterStd[1] = Math.sqrt(tQuarterStd[1]/(maxTimes - 1));
		tQuarterStd[2] = Math.sqrt(tQuarterStd[2]/(maxTimes - 1));
		
		vQuarterStd[0] = Math.sqrt(vQuarterStd[0]/(maxTimes - 1));
		vQuarterStd[1] = Math.sqrt(vQuarterStd[1]/(maxTimes - 1));
		vQuarterStd[2] = Math.sqrt(vQuarterStd[2]/(maxTimes - 1));
		
		try
		{
			BufferedWriter avgResult = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("avgResult.txt")));
			
			avgResult.write("Quarter Result:\n");
			avgResult.write("Treding:\n");
			avgResult.write(tQuarterResult[0] + "+-" + tQuarterStd[0] + "\t" + tQuarterResult[1] + "+-" + tQuarterStd[1] + "\t" + tQuarterResult[2] + "+-" + tQuarterStd[2] + "\n");
			avgResult.write("MAE:\n");
			avgResult.write(vQuarterResult[0] + "+-" + vQuarterStd[0] + "\t" + vQuarterResult[1] + "+-" + vQuarterStd[1] + "\t" + vQuarterResult[2] + "+-" + vQuarterStd[2] + "\n");
			
			avgResult.write("\n");
			avgResult.write("Trending:\t" + tResult + "+-" + tStd + "\n");
			avgResult.write("MAE:\t" + vResult + "+-" + vStd + "\t");
			
			avgResult.close();
		}
		catch(FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void testResampling()
	{
		try 
		{
			BufferedWriter testing = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("testing.txt")));
			
			init(100);
			testing.write("Initial\n");
			for(int i = 0; i < particle.size(); i++)
			{
				testing.write(particle.get(i) + "\t" + weight.get(i) + "\n");
			}
			testing.write("\n");
			
			double referenceValue = 0.6;
			

			sense(referenceValue);
			testing.write("Sensing\n");
			for(int i = 0; i < particle.size(); i++)
			{
				testing.write(particle.get(i) + "\t" + weight.get(i) + "\n");
			}
			testing.write("\n");
			
			resamplingTesting(testing);
			testing.write("Resampling\n");
			for(int i = 0; i < particle.size(); i++)
			{
				testing.write(particle.get(i) + "\t" + weight.get(i) + "\n");
			}
			testing.write("\n");
			
			move();
			testing.write("Move\n");
			for(int i = 0; i < particle.size(); i++)
			{
				testing.write(particle.get(i) + "\t" + weight.get(i) + "\n");
			}
			testing.write("\n");
			
			testing.close();
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
	
	public static void parameterChooser()
	{
		ParticleFilterEngine engine = new ParticleFilterEngine();
		
		int[] particle = {10, 30, 50, 70, 100};
		int[] day = {4, 6, 8, 10};
		
		class Setting
		{
			int particle;
			int day;
			ArrayList<Double> tValue; 
			ArrayList<Double> vError;
			
			Setting()
			{
				tValue = new ArrayList<Double>();
				vError = new ArrayList<Double>();
			}
			
			Setting(int particle, int day)
			{
				this.particle = particle;
				this.day = day;
				
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
			
			public double stdTValue()
			{
				double average = avgTValue();
				
				double sum = 0;
				for(int i = 0; i < tValue.size(); i++)
				{
					sum += Math.pow((tValue.get(i) - average), 2);
				}
				
				return Math.sqrt(sum/(tValue.size() - 1));
			}
			
			public double stdVError()
			{
				double average = avgVError();
				
				double sum = 0;
				for(int i = 0; i < vError.size(); i++)
				{
					sum += Math.pow((vError.get(i) - average), 2);
				}
				
				return Math.sqrt(sum/(vError.size() - 1));
			}
		}
		
		ArrayList<Setting> settingList = new ArrayList<Setting>();
		
		for(int i = 0; i < particle.length; i++)
		{
			for(int j = 0; j < day.length; j++)
			{
				Setting newSetting = new Setting(particle[i], day[j]);
					
				settingList.add(newSetting);
			}
		}
		
		ArrayList<Double> data = readFileToDouble("Data - VNINDEX.txt");
		
		for(int times = 0; times < 100; times++)
		{
			System.out.println("Runing times: " + (times + 1));
			for(int i = 0; i < settingList.size(); i++)
			{
				System.out.println(
						settingList.get(i).particle + " - " +
						settingList.get(i).day);
				
				ArrayList<Double> subdata = new ArrayList<Double>(data.subList(10 - settingList.get(i).day, 10));
				
				double predictValue = engine.predict(subdata, settingList.get(i).particle);
				double previousPredictValue = predictValue;
				
				double tValue = 0;
				double vError = 0;
				
				double count = 0;
				
				for(int date = 11; date < 619; date++)
				{
					subdata = new ArrayList<Double>(data.subList(date - settingList.get(i).day, date));
					predictValue = engine.predict(subdata, settingList.get(i).particle);
					
					vError += Math.abs(predictValue - data.get(date));
					
					if((predictValue - previousPredictValue) * (data.get(date) - data.get(date - 1)) > 0)
					{
						tValue++;
					}
					
					previousPredictValue = predictValue;
					count++;
				}
				
				settingList.get(i).tValue.add(1 - (tValue/count));
				settingList.get(i).vError.add(vError/count);
			}
		}
		
		try 
		{
			BufferedWriter settingStatistics = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("settingStatistics.txt")));
			
			settingStatistics.write("id" + "\t" + "particle" + "\t" + "day" + "\t" + "avgTValue" + "\t" + "stdTvalue" + "\t" + "avgVError" + "\t" + "stdVError" + "\n");
			
			for(int i = 0; i < settingList.size(); i++)
			{
				settingStatistics.write((i + 1) + 
						"\t" + settingList.get(i).particle + 
						"\t" + settingList.get(i).day + 
						"\t" + settingList.get(i).avgTValue() +
						"\t" + settingList.get(i).stdTValue() +
						"\t" + settingList.get(i).avgVError() + 
						"\t" + settingList.get(i).stdVError() + "\n");
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
	
	public static void main(String args[])
	{
//		ParticleFilterEngine engine = new ParticleFilterEngine();
//		
//		ArrayList<Double> data = readFileToDouble("Data - VNINDEX.txt");
//		
//		ArrayList<Double> subdata = new ArrayList<Double>();
//			
//		subdata.addAll(data.subList(623 - 8, 623));
//		
//		engine.predictTesting(subdata, 100);
 		
		testingResult(100);	
		
//		parameterChooser();
	}
}
