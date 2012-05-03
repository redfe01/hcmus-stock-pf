package org.hcmus.stock.particlefilter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import umontreal.iro.lecuyer.probdist.UniformDist;

public class CurveParticleFilterEngine 
{
	private ArrayList<CurveParticle> particleList;
	private ArrayList<Double> referenceData;
	private int startDate;
	private int endDate;
	private int curveLength;
	private int numParticle;
	
	public CurveParticleFilterEngine(ArrayList<Double> inputReferenceData, int inputStartDate, int inputEndDate, int inputCurveLength, int inputNumParticle)
	{
		particleList = new ArrayList<CurveParticle>();
		referenceData = inputReferenceData;
		startDate = inputStartDate;
		endDate = inputEndDate;
		curveLength = inputCurveLength;
		numParticle = inputNumParticle;
	}
	
	private void particleGeneratorFirstOrder()
	{
		for(int i = 0; i < numParticle; i++)
		{	
			ArrayList<Double> newDataValue = new ArrayList<Double>();
			for(int j = 0; j < curveLength - 1; j++)
			{
				newDataValue.add(referenceData.get(endDate - (curveLength - 1) + j));
			}
			
			Random generator = new Random();
			
			double differences = 0;
			
			double probability = generator.nextDouble();
			
			if(probability > 0.5)
			{
				differences = referenceData.get(endDate - 1) * 0.05 * probability;
			}
			else
			{
				differences = - (referenceData.get(endDate - 1) * 0.05 * (1 - probability));
			}
			
			newDataValue.add(referenceData.get(endDate - 1) + differences);
			
			/*	
			double max = referenceData.get(endDate - 1) * 0.05;
			double min = -referenceData.get(endDate - 1) * 0.05;
			
			UniformDist uniDist = new UniformDist(min, max);
			
			newDataValue.add(referenceData.get(endDate - 1) + uniDist.inverseF(generator.nextDouble()));
			*/

			CurveParticle newParticle = new CurveParticle(newDataValue);
			
			particleList.add(newParticle);
		}
	}
	
	private void calculateWeight()
	{
		for(int i = 0; i < particleList.size(); i++)
		{
			ArrayList<Double> score = new ArrayList<Double>();
			for(int j = startDate - 1 ; j < endDate - (curveLength - 1); j++)
			{
				ArrayList<Double> referenceCurve = new ArrayList<Double>();
				for(int k = j; k < j + curveLength; k++)
				{
					referenceCurve.add(referenceData.get(k));
				}
				
				score.add(euclideanDistance(normalize(particleList.get(i).getDataValue()), normalize(referenceCurve)));
			}
			
			double minScore = Collections.min(score);
			
			if(minScore < 0.5)
			{
				particleList.get(i).setWeightValue(1/Collections.min(score));
			}
			else
			{
				particleList.remove(i);
				i--;
			}
		}
	}
	
	private void normalizeWeight()
	{
		double sum = 0;
		for(int i = 0; i < particleList.size(); i++)
		{	
			sum += particleList.get(i).getWeightValue();
		}
		
		for(int i = 0; i < particleList.size(); i++)
		{
			double newWeightValue = particleList.get(i).getWeightValue()/sum;
			particleList.get(i).setWeightValue(newWeightValue);
		}
	}
	
	public double doPrediction()
	{
		int flag = 0;
		
		double predictedValue = 0;
		
		for(int times = 0; times < 20; times++)
		{
			particleList.clear();
			particleGeneratorFirstOrder();
			calculateWeight();
			
			if(particleList.size() > 10)
			{
				flag = 1;
				break;
			}
		}	
		
		if(flag == 1)
		{
			normalizeWeight();
			
			for(int i = 0; i < particleList.size(); i++)
			{
				 predictedValue += particleList.get(i).getDataValue().get(curveLength - 1) * particleList.get(i).getWeightValue();
			}	
		}

		return predictedValue;
	}
	
	private double euclideanDistance(ArrayList<Double> x1, ArrayList<Double> x2) 
	{
		if (x1.size() != x2.size()) 
		{
			return 0;
		}
		
		double result = 0;
		
		for (int i = 0; i < x1.size(); ++i) 
		{
			result += Math.pow(x1.get(i) - x2.get(i),2);
		}
		
		return Math.sqrt(result);
	}
	
	private double mean (ArrayList<Double> x) 
	{
		double mean = 0;
		for (Double e : x) 
		{
			mean += e;
		}
		mean /= x.size();
		return mean;
	}
	
	private double std (ArrayList<Double> x) 
	{
		double variance = 0;
		double mean = mean(x);
		for (Double e : x) 
		{
			variance += (e - mean)*(e - mean);
		}
		variance /= x.size();
		return Math.sqrt(variance);
	}
	
	private ArrayList<Double> normalize(ArrayList<Double> x) 
	{
		double mean = mean(x);
		double std = std(x);
		
		ArrayList<Double> result = new ArrayList<Double>();
		
		for (int i = 0; i < x.size(); ++i) 
		{
			result.add((x.get(i) - mean)/std);
		}
		
		return result;
	}
	
	public void setParticleList(ArrayList<CurveParticle> particleList) 
	{
		this.particleList = particleList;
	}
	
	public ArrayList<CurveParticle> getParticleList() 
	{
		return particleList;
	}

	public void setReferenceUnit(ArrayList<Double> referenceUnit) 
	{
		this.referenceData = referenceUnit;
	}

	public ArrayList<Double> getReferenceUnit() 
	{
		return referenceData;
	}

	public void setCurveLength(int curveLength) 
	{
		this.curveLength = curveLength;
	}

	public int getCurveLength() 
	{
		return curveLength;
	}
	
	public void setStartDate(int startDate) 
	{
		this.startDate = startDate;
	}

	public int getStartDate() 
	{
		return startDate;
	}

	public void setEndDate(int endDate) 
	{
		this.endDate = endDate;
	}

	public int getEndDate() 
	{
		return endDate;
	}
	
	public void setNumParticle(int numParticle) {
		this.numParticle = numParticle;
	}

	public int getNumParticle() {
		return numParticle;
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
		ArrayList<Double> referenceData = readFileToDouble("Data - VNINDEX.txt");
		
		/*
		for(int i = 2; i < referenceData.size(); i++)
		{
			if(referenceData.get(i - 1) - referenceData.get(i - 2) < 0)
			{
				System.out.println(-1);
			}
			else
			{
				System.out.println(1);
			}
		}
		*/
		
		int endDate = 2000;
		
		CurveParticleFilterEngine pfEngine = new CurveParticleFilterEngine(referenceData, 1, endDate, 5, 100);
		
		for(int i = 0; i < 500; i++)
		{
			System.out.println(pfEngine.doPrediction());
			endDate++;
			pfEngine.setEndDate(endDate);
		}
		
		
		/*
		ArrayList<Double> testData = new ArrayList<Double>();
		
		testData.add(475.1);
		testData.add(489.3);
		testData.add(483.5);
		testData.add(456.4);
		testData.add(467.8);
		testData.add(478.9);
		
		CurveParticleFilterEngine test = new CurveParticleFilterEngine(testData, 1, 6, 4, 5);
		test.particleGeneratorFirstOrder();
		
		for(int i = 0; i < test.getParticleList().size(); i++)
		{
			for(int j = 0; j < test.getParticleList().get(i).getDataValue().size(); j++)
			{
				System.out.println(test.getParticleList().get(i).getDataValue().get(j) + " ");
			}
			System.out.println();
		}
		
		*/
	}

}
