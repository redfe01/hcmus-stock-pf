package org.hcmus.stock.particlefilter;

public class NumericalParticle 
{
	private double dataValue;
	private	double weightValue;
	
	NumericalParticle(double inputDataValue)
	{
		dataValue = inputDataValue;
		weightValue = 0;
	}
	
	public void setWeightValue(double weightValue) 
	{
		this.weightValue = weightValue;
	}
	
	public double getWeightValue() 
	{
		return weightValue;
	}

	public void setDataValue(double dataValue) {
		this.dataValue = dataValue;
	}

	public double getDataValue() {
		return dataValue;
	}
	
}

