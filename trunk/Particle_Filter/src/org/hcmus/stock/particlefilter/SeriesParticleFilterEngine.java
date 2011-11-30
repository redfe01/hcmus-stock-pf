package org.hcmus.stock.particlefilter;

import java.util.ArrayList;
import java.util.Random;

public class SeriesParticleFilterEngine 
{
	private ArrayList<Double> referenceData;
	private int numParticle;
	private double threshold;
	private int startDate;
	private int endDate;
	
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
	
	private ArrayList<Double> functionGeneration(double anchorDate, double referenceDate, int numParticle, double threshold)
	{
		ArrayList<Double> result = new ArrayList<Double>();
		
		for(int loop = 0; loop < 10; loop++)
		{
			double functionError = 0;
			
			for(int i = 0; i < numParticle; i++)
			{
				Random generator = new Random();
				
				double differences = 0;
				
				double probability = generator.nextDouble();
				
				if(probability > 0.5)
				{
					differences = anchorDate * 0.05 * probability;
				}
				else
				{
					differences = - (anchorDate * 0.05 * (1 - probability));
				}
				
				result.add(anchorDate + differences);
			}
			
			for(int i = 0; i < numParticle; i++)
			{
				functionError += Math.abs(result.get(i) - referenceDate);
			}
			
			functionError = functionError / numParticle;
			
			if(functionError < threshold)
				break;
			
		}
		
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
}
