package org.hcmus.stock.particlefilter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Data 
{
	private ArrayList<Double> data; 
	private ArrayList<Double> ma_data;
	private ArrayList<Double> data_whole;
	private double data_mu;
	private double data_sigma;
	private double ma_mu;
	private double ma_sigma;
	private int period;
	
	public Data()
	{
		data = new ArrayList<Double>();
		ma_data = new ArrayList<Double>();
		data_whole = new ArrayList<Double>();
		data_mu = 0;
		data_sigma = 0;
		ma_mu = 0;
		ma_sigma = 0;
		period = 0;
	}
	
	public Data(String inputFilePath, int period)
	{
		data = new ArrayList<Double>();
		data_whole = new ArrayList<Double>();
		ma_data = new ArrayList<Double>();
		data_mu = 0;
		data_sigma = 0;
		ma_mu = 0;
		ma_sigma = 0;
		this.period = period;
		
		getDataWhole(inputFilePath);
	}
	
	public Data(String inputFilePath, int startPoint, int endPoint, int period)
	{
		data = new ArrayList<Double>();
		data_whole = new ArrayList<Double>();
		ma_data = new ArrayList<Double>();
		data_mu = 0;
		data_sigma = 0;
		ma_mu = 0;
		ma_sigma = 0;
		this.period = period;
		
		getDataWhole(inputFilePath);
		
		if(startPoint == 0 && endPoint == 0)
		{
			data.addAll(data_whole);
		}
		else
		{
			getDataSegment(startPoint, endPoint);
		}
		getMuFromData();
		getSigmaFromData();
		getMaData(period);
		getMAMuFromData();
		getMASigmaFromData();
	}
	
	public double getScaleParameterRayleighDist()
	{
		double sum = 0;
		for(int i = 0; i < data.size(); i++)
		{
			sum += Math.pow(data.get(i),2);
		}
		
		sum = Math.sqrt(sum/(2*data.size()));
		
		return sum;
	}
	
	private void getMuFromData()
	{
		double sum = 0;
		for(int i = 0; i < data.size(); i++)
		{
			sum += data.get(i);
		}
		data_mu = sum / data.size();
	}
	
	private void getSigmaFromData()
	{		
		double sum = 1e-5;
		
		for(int i = 0; i < data.size(); i++)
		{
			sum += Math.pow(data.get(i) - data_mu, 2);
		}
		
		data_sigma = Math.sqrt(sum / data.size());
	}
	
	private void getMaData(int period)
	{
		for(int i = period - 1; i < data.size(); i++)
		{
			double sum = 0;
			for(int j = i - period + 1; j < i + 1; j++)
			{
				sum += data.get(j);
			}
			ma_data.add(sum / period);
		}
	}
	
	private void getMAMuFromData()
	{
		double sum = 0;
		for(int i = 0; i < ma_data.size(); i++)
		{
			sum += ma_data.get(i);
		}
		ma_mu = sum / ma_data.size();
	}
	
	private void getMASigmaFromData()
	{
		double sum = 0;
		for(int i = 0; i < ma_data.size(); i++)
		{
			sum += Math.pow(ma_data.get(i) - ma_mu, 2);
		}
		ma_sigma = sum / ma_data.size();
	}
	
	private void getDataWhole(String inputFilePath)
	{
		try 
		{
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFilePath)));
			
			String line = null;
			
			try 
			{
				while((line = reader.readLine()) != null)
				{
					data_whole.add(Double.parseDouble(line));
				}
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
			
			reader.close();
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
	
	private void getDataSegment(int startPoint, int endPoint)
	{
		if(startPoint > data_whole.size() - 1)
		{
			System.out.println("Invalid start point");
			return;
		}
		
		if(endPoint < startPoint)
		{
			System.out.println("End point cannot smaller than start point");
		}
		
		if(endPoint > data_whole.size() - 1)
		{
			System.out.println("Invalid end point");
			return;
		}
		
		for(int i = startPoint - 1; i < endPoint; i++)
		{
			data.add(data_whole.get(i));
		}
	}
	
	public void updataData(int startPoint, int endPoint)
	{
		data.clear();
		getDataSegment(startPoint, endPoint);
		getMuFromData();
		getSigmaFromData();
		getMaData(period);
		getMAMuFromData();
		getMASigmaFromData();
	}
	
	public void updataData(ArrayList<Double> inputData)
	{
		data.clear();
		data.addAll(inputData);
		getMuFromData();
		getSigmaFromData();
		getMaData(period);
		getMAMuFromData();
		getMASigmaFromData();
	}
	
	public void updataMAData(int period)
	{
		ma_data.clear();
		this.period = period;
		getMaData(period);
		getMAMuFromData();
		getMASigmaFromData();
	}
	
	public double getAverage() 
	{
		return data_mu;
	}
		
	public double getVariance() 
	{
		return data_sigma;
	}
	
	public double getMa_average() 
	{
		return ma_mu;
	}
	
	public double getMa_variance() {
		return ma_sigma;
	}

	public ArrayList<Double> getData() 
	{
		return data;
	}

	public ArrayList<Double> getMa_data() 
	{
		return ma_data;
	}
	
	public ArrayList<Double> getData_whole() 
	{
		return data_whole;
	}

	public int getPeriod() 
	{
		return period;
	}
	
	public void setData_whole(ArrayList<Double> data_whole) 
	{
		this.data_whole = data_whole;
	}

} 
