package org.hcmus.stock.particlefilter;

import java.util.ArrayList;

public class ParticleFilter 
{
	private ArrayList<Double> particleList;
	private ArrayList<Double> weightList;
	
	ParticleFilter()
	{
		particleList = new ArrayList<Double>();
		weightList = new ArrayList<Double>();
	}
	
	ParticleFilter(int n)
	{
		double probDelta = 0.1 / (n * 1.0);
		
		for(int i = 0; i < n; i++)
		{
			particleList.add(-0.05 + (probDelta * i));
		}
		
		for(int i = 0; i < n; i++)
		{
			weightList.add(1.0 / (n * 1.0));
		}
	}
	
	private void normalize()
	{
		double sum = 0;
		for(int i = 0; i < weightList.size(); i++)
		{
			sum += weightList.get(i);
		}
		
		for(int i = 0; i < weightList.size(); i++)
		{
			weightList.set(i, weightList.get(i) / sum);
		}
	}
	
	public void sense(double pricePrevDay, double priceCurrDay)
	{	
		double distance = Math.abs(priceCurrDay - pricePrevDay);
		
		for(int i = 0; i < particleList.size(); i++)
		{
			double different = Math.abs(distance - particleList.get(i));
			
			weightList.set(i, 1.0/different);
		}
		
		normalize();
		
		
		
		
	}
	
	
	
	
}
