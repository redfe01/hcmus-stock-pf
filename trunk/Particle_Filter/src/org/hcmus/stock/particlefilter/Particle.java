package org.hcmus.stock.particlefilter;

public class Particle implements Comparable<Particle>
{
	private double particleValue;
	private double particleWeight;
	
	public Particle(){}

	public double getParticleValue() 
	{
		return particleValue;
	}

	public void setParticleValue(double particleValue) 
	{
		this.particleValue = particleValue;
	}

	public double getParticleWeight() 
	{
		return particleWeight;
	}

	public void setParticleWeight(double particleWeight) 
	{
		this.particleWeight = particleWeight;
	}

	@Override
	public int compareTo(Particle arg0) 
	{
		if(particleWeight > arg0.particleWeight)
			return 1;
		
		if(particleWeight == arg0.particleWeight)
			return 0;
			
		if(particleWeight < arg0.particleWeight)
			return -1;
		
		return 0;
	}
}
