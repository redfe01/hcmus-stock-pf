package org.hcmus.stock.particlefilter;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import umontreal.iro.lecuyer.probdist.NormalDist;
import umontreal.iro.lecuyer.probdist.RayleighDist;

public class ParticleFilter 
{
	private ArrayList<Double> particleList;
	private ArrayList<Double> weight;
	
	public ParticleFilter()
	{
		particleList = new ArrayList<Double>();
		weight = new ArrayList<Double>();
	}
	
	/*
	private void doPredicTionWithFE()
	{
		FiniteElements predictFunction = new FiniteElements(historicalData.getData(), 1);
		try 
		{
			predictingPoint = predictFunction.runAlgorithm();
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	*/
	
	private void particleGenerator(double mean, double sigma, int numPar)
	{
		double particle = 0;
		for(int i = 0; i < numPar; i++)
		{
			particle = NormalDist.inverseF(mean, sigma, Math.random());
			particleList.add(particle);
		}
	}
	
	private void particleGeneratorRayleighDist(double beta, int numPar)
	{
		double particle = 0;
		for(int i = 0; i < numPar; i++)
		{
			particle = RayleighDist.inverseF(0, beta, Math.random());
			particleList.add(particle);
		}
	}
	
	private void weightByDistance(double predictedPoint)
	{
		double distance = 0;
		for(int i = 0; i < particleList.size(); i++)
		{
			distance = Math.abs(predictedPoint - particleList.get(i));
			weight.add(1/(distance/predictedPoint));
		}
		
		weightNormalize();
	}
	
	private void weightNormalize()
	{
		double sum = 0;
		for(int i = 0; i < weight.size(); i++)
		{
			sum += weight.get(i);
		}
		
		for(int i = 0; i < weight.size(); i++)
		{
			weight.set(i, weight.get(i)/sum);
		}
	}
	
	public double doPrediction(double predictedPoint, double mean, double sigma, int numPar)
	{
		particleList.clear();
		weight.clear();
		particleGenerator(mean, sigma, numPar);
		weightByDistance(predictedPoint);
		weightNormalize();
		
		double sum = 0;
		for(int i = 0; i < particleList.size(); i++)
		{
			sum += particleList.get(i) * weight.get(i);
		}
		
		return sum;
	}
	
	public double doPredictionRayLeighDist(double referencePoint, double beta, int numPar)
	{	
		particleList.clear();
		weight.clear();
		particleGeneratorRayleighDist(beta, numPar);
		weightByDistance(referencePoint);
		weightNormalize();
		
		double result = 0;
		
		for(int i = 0; i < particleList.size(); i++)
		{
			result += particleList.get(i) * weight.get(i);
		}
		
		return result;
	}
	
	public ArrayList<Double> getParticle() {
		return particleList;
	}
	
	public void setParticle(ArrayList<Double> particle) {
		this.particleList = particle;
	}
	
	public ArrayList<Double> getWeight() {
		return weight;
	}
	
	public void setWeight(ArrayList<Double> weight) {
		this.weight = weight;
	}
	
	public static void main(String[] args)
	{
		ParticleFilter pfEngine = new ParticleFilter();
		Data testData = new Data("AGF.txt", 5);
		
		//NumberFormat formatter = new DecimalFormat("#.#######");
		
		ArrayList<Double> dataPrediction = new ArrayList<Double>();
		
		for(int i = 0; i < 300; i++)
		{
			dataPrediction.add(0.0);
		}
		
		try
		{
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("D:\\AFG_result.txt")));
			
			for(int i = 0; i < 20; i++)
			{
				for(int j = 0; j < 300; j++)
				{
					int beginday = 1 + j;
					int lastday = 3 + j;
					testData.updataData(beginday, lastday);

					if(i == 0)
					{
						//dataPrediction.set(j, pfEngine.doPrediction(testData.getData().get(lastday - beginday), testData.getAverage(), testData.getVariance(), 500));
						dataPrediction.set(j, pfEngine.doPredictionRayLeighDist(testData.getData().get(lastday - beginday), testData.getScaleParameterRayleighDist(), 500));
					}
					else
					{
						//dataPrediction.set(j, dataPrediction.get(j) + pfEngine.doPrediction(testData.getData().get(lastday - beginday), testData.getAverage(), testData.getVariance(), 500));
						dataPrediction.set(j, dataPrediction.get(j) + pfEngine.doPredictionRayLeighDist(testData.getData().get(lastday - beginday), testData.getScaleParameterRayleighDist(), 500));
					}
					
					/*
					if(j == 0)
					{
						writer.write(formatter.format(pfEngine.doPrediction(
								testData.getData().get(lastday - 1), 
								testData.getAverage(), 
								testData.getVariance(),
								500
								)));
					}
					else
					{
						writer.write("\t" + formatter.format(pfEngine.doPrediction(
								testData.getData().get(lastday - 1), 
								testData.getAverage(), 
								testData.getVariance(),
								500
								)));
					}
					//end previous block comment here
					*/
				}
			}
			
			for(int i = 0; i < dataPrediction.size(); i++)
			{
				dataPrediction.set(i, dataPrediction.get(i)/20);
			}
			
			for(int i = 0; i < dataPrediction.size(); i++)
			{
				writer.write(dataPrediction.get(i) + "\n");
			}
			
			writer.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
}
