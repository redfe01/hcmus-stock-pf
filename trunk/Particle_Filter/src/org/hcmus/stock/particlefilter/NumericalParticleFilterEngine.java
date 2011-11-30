package org.hcmus.stock.particlefilter;

import java.util.ArrayList;

import umontreal.iro.lecuyer.probdist.NormalDist;
import umontreal.iro.lecuyer.probdist.RayleighDist;

public class NumericalParticleFilterEngine 
{
	private ArrayList<NumericalParticle> particleList;
	
	public NumericalParticleFilterEngine()
	{
		particleList = new ArrayList<NumericalParticle>();
	}
	
	private void particleGeneratorNormalDist(double mean, double sigma, int numPar)
	{
		double dataValue = 0;
		for(int i = 0; i < numPar; i++)
		{
			dataValue = NormalDist.inverseF(mean, sigma, Math.random());
			
			NumericalParticle newParticle = new NumericalParticle(dataValue);
			particleList.add(newParticle);
		}
	}
	
	private void particleGeneratorRayleighDist(double beta, int numPar)
	{
		double dataValue = 0;
		for(int i = 0; i < numPar; i++)
		{
			dataValue = RayleighDist.inverseF(0, beta, Math.random());

			NumericalParticle newParticle = new NumericalParticle(dataValue);
			particleList.add(newParticle);
		}
	}
	
	private void weightByDistance(double referencedPoint)
	{
		double distance = 0;
		for(int i = 0; i < particleList.size(); i++)
		{
			distance = Math.abs(referencedPoint - particleList.get(i).getDataValue());
			particleList.get(i).setWeightValue(1/(distance/referencedPoint));
		}
		
		weightNormalize();
	}
	
	private void weightNormalize()
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
	
	public double doPrediction(double referencedPoint, double mean, double sigma, int numPar)
	{
		particleList.clear();
		particleGeneratorNormalDist(mean, sigma, numPar);
		weightByDistance(referencedPoint);
		weightNormalize();
		
		double sum = 0;
		for(int i = 0; i < particleList.size(); i++)
		{
			sum += particleList.get(i).getDataValue() * particleList.get(i).getWeightValue();
		}
		
		return sum;
	}
	
	public double doPredictionRayLeighDist(double referencedPoint, double beta, int numPar)
	{	
		particleList.clear();
		particleGeneratorRayleighDist(beta, numPar);
		weightByDistance(referencedPoint);
		weightNormalize();
		
		double result = 0;
		
		for(int i = 0; i < particleList.size(); i++)
		{
			result += particleList.get(i).getDataValue() * particleList.get(i).getWeightValue();
		}
		
		return result;
	}
	
	public ArrayList<NumericalParticle> getParticle() {
		return particleList;
	}
	
	public void setParticle(ArrayList<NumericalParticle> particle) {
		this.particleList = particle;
	}
	
/*	
 * public static void main(String[] args)
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
*/

}
