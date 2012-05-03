package org.hcmus.stock.particlefilter;

import java.util.ArrayList;

public class CurveParticle 
{
	private ArrayList<Double> dataValue;
	private double weightValue;
	
	public CurveParticle(ArrayList<Double> inputDataValue)
	{
		dataValue = inputDataValue;
		weightValue = 0;
	}
	
	public void setDataValue(ArrayList<Double> dataValue) 
	{
		this.dataValue = dataValue;
	}
	
	public ArrayList<Double> getDataValue() 
	{
		return dataValue;
	}

	public void setWeightValue(double weightValue) 
	{
		this.weightValue = weightValue;
	}

	public double getWeightValue() 
	{
		return weightValue;
	}

}
