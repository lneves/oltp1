package org.oltp1.egen.io;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.oltp1.egen.generator.CompanyFile;
import org.oltp1.egen.generator.SecurityFile;
import org.oltp1.egen.generator.TaxRateFile;
import org.oltp1.egen.io.records.AreaCodeDataFileRecord;
import org.oltp1.egen.io.records.ChargeDataFileRecord;
import org.oltp1.egen.io.records.CommissionRateDataFileRecord;
import org.oltp1.egen.io.records.CompanyCompetitorDataFileRecord;
import org.oltp1.egen.io.records.CompanyDataFileRecord;
import org.oltp1.egen.io.records.CompanySpRateDataFileRecord;
import org.oltp1.egen.io.records.ExchangeDataFileRecord;
import org.oltp1.egen.io.records.FemaleFirstNameDataFileRecord;
import org.oltp1.egen.io.records.IndustryDataFileRecord;
import org.oltp1.egen.io.records.LastNameDataFileRecord;
import org.oltp1.egen.io.records.MaleFirstNameDataFileRecord;
import org.oltp1.egen.io.records.NewsDataFileRecord;
import org.oltp1.egen.io.records.NonTaxableAccountNameDataFileRecord;
import org.oltp1.egen.io.records.SectorDataFileRecord;
import org.oltp1.egen.io.records.SecurityDataFileRecord;
import org.oltp1.egen.io.records.StatusTypeDataFileRecord;
import org.oltp1.egen.io.records.StreetNameDataFileRecord;
import org.oltp1.egen.io.records.StreetSuffixDataFileRecord;
import org.oltp1.egen.io.records.TaxRateCountryDataFileRecord;
import org.oltp1.egen.io.records.TaxRateDivisionDataFileRecord;
import org.oltp1.egen.io.records.TaxableAccountNameDataFileRecord;
import org.oltp1.egen.io.records.TradeTypeDataFileRecord;
import org.oltp1.egen.io.records.ZipCodeDataFileRecord;

/**
 * Manages the loading and parsing of all TPC-E flat input files. This class
 * uses lazy loading: each file is read from disk only when its data is first
 * requested. This mirrors the design of the original C++ DataFileManager. Based
 * on InputFiles/inc/DataFileManager.h and its .cpp file.
 */
public class DataFileManager
{
	private final long configuredCustomers;
	private final long activeCustomers;

	private List<ChargeDataFileRecord> chargeDataFile;
	private List<CommissionRateDataFileRecord> commissionRateDataFile;
	private List<IndustryDataFileRecord> industryDataFile;
	private List<SectorDataFileRecord> sectorDataFile;
	private TaxRateFile taxRateFile;
	private List<TradeTypeDataFileRecord> tradeTypeDataFile;

	private List<StatusTypeDataFileRecord> statusTypeDataFile;
	private List<ExchangeDataFileRecord> exchangeDataFile;
	private List<CompanyDataFileRecord> companyDataFile;

	private List<TaxableAccountNameDataFileRecord> taxableAccountNameDataFile;
	private List<NonTaxableAccountNameDataFileRecord> nonTaxableAccountNameDataFile;
	private List<CompanyCompetitorDataFileRecord> companyCompetitorDataFile;
	private List<SecurityDataFileRecord> securityDataFile;
	private SecurityFile securityFile;

	// Weighted Files
	private WeightedDataFile<LastNameDataFileRecord> lastNameDataFile;
	private WeightedDataFile<AreaCodeDataFileRecord> areaCodeDataFile;
	private WeightedDataFile<FemaleFirstNameDataFileRecord> femaleFirstNameDataFile;
	private WeightedDataFile<MaleFirstNameDataFileRecord> maleFirstNameDataFile;
	private WeightedDataFile<StreetNameDataFileRecord> streetNameDataFile;
	private WeightedDataFile<StreetSuffixDataFileRecord> streetSuffixDataFile;
	private WeightedDataFile<ZipCodeDataFileRecord> zipCodeDataFile;
	private WeightedDataFile<CompanySpRateDataFileRecord> companySpRateDataFile;
	private WeightedDataFile<NewsDataFileRecord> newsDataFile;

	// Add private fields for the new bucketed data files
	private BucketedDataFile<TaxRateCountryDataFileRecord> taxRateCountryDataFile;
	private BucketedDataFile<TaxRateDivisionDataFileRecord> taxRateDivisionDataFile;

	private CompanyFile companyFile;

	public DataFileManager(long configuredCustomers, long activeCustomers)
	{
		this.configuredCustomers = configuredCustomers;
		this.activeCustomers = activeCustomers;
	}

	public long getConfiguredCustomers()
	{
		return configuredCustomers;
	}

	public long getActiveCustomers()
	{
		return activeCustomers;
	}

	// A generic helper method to load, parse, and cache a data file.

	private <T> List<T> loadDataFile(String fileName, Function<String, T> parser)
	{
		System.out.printf("%nLoading input file: %s%n", fileName);

		String resourcePath = String.format("/flat_in/%s", fileName);

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(resourcePath))))
		{
			return reader
					.lines()
					.filter(line -> !line.trim().isEmpty()) // Skip blank lines
					.map(parser)
					.collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
		}
		catch (Throwable t)
		{
			// A missing/unreadable input file is a fatal configuration error.
			throw new RuntimeException("Failed to load input data file: " + fileName, t);
		}
	}

	// An overloaded helper for loading WEIGHTED data files.
	private <T> WeightedDataFile<T> loadWeightedDataFile(String fileName, Function<String, T> parser, Function<T, Integer> weightExtractor)
	{
		System.out.printf("%nLoading weighted input file: %s%n", fileName);

		String resourcePath = String.format("/flat_in/%s", fileName);

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(resourcePath))))
		{
			List<String> lines = reader
					.lines()
					.collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
			return new WeightedDataFile<>(lines, parser, weightExtractor);
		}
		catch (Throwable t)
		{
			throw new RuntimeException("Failed to load weighted input data file: " + fileName, t);
		}
	}

	// An overloaded helper for loading BUCKETED data files.
	private <T> BucketedDataFile<T> loadBucketedDataFile(String fileName, Function<Deque<String>, T> parser)
	{
		System.out.printf("%nLoading bucketed input file: %s%n", fileName);

		String resourcePath = String.format("/flat_in/%s", fileName);

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(resourcePath))))
		{
			List<String> lines = reader
					.lines()
					.collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
			return new BucketedDataFile<>(lines, parser);
		}
		catch (Throwable t)
		{
			throw new RuntimeException("Failed to load bucketed input data file: " + fileName, t);
		}
	}

	public WeightedDataFile<LastNameDataFileRecord> getLastNameDataFile()
	{
		if (lastNameDataFile == null)
		{
			lastNameDataFile = loadWeightedDataFile("LastName.txt", LastNameDataFileRecord::parse, record -> record.weight);
		}
		return lastNameDataFile;
	}

	public WeightedDataFile<AreaCodeDataFileRecord> getAreaCodeDataFile()
	{
		if (areaCodeDataFile == null)
		{
			areaCodeDataFile = loadWeightedDataFile("AreaCode.txt", AreaCodeDataFileRecord::parse, record -> record.weight);
		}
		return areaCodeDataFile;
	}

	public WeightedDataFile<MaleFirstNameDataFileRecord> getMaleFirstNameDataFile()
	{
		if (maleFirstNameDataFile == null)
		{
			maleFirstNameDataFile = loadWeightedDataFile("MaleFirstName.txt", MaleFirstNameDataFileRecord::parse, record -> record.weight);
		}
		return maleFirstNameDataFile;
	}

	public WeightedDataFile<FemaleFirstNameDataFileRecord> getFemaleFirstNameDataFile()
	{
		if (femaleFirstNameDataFile == null)
		{
			femaleFirstNameDataFile = loadWeightedDataFile("FemaleFirstName.txt", FemaleFirstNameDataFileRecord::parse, record -> record.weight);
		}
		return femaleFirstNameDataFile;
	}

	public WeightedDataFile<StreetNameDataFileRecord> getStreetNameDataFile()
	{
		if (streetNameDataFile == null)
		{
			streetNameDataFile = loadWeightedDataFile("StreetName.txt", StreetNameDataFileRecord::parse, record -> record.weight);
		}
		return streetNameDataFile;
	}

	public WeightedDataFile<StreetSuffixDataFileRecord> getStreetSuffixDataFile()
	{
		if (streetSuffixDataFile == null)
		{
			streetSuffixDataFile = loadWeightedDataFile("StreetSuffix.txt", StreetSuffixDataFileRecord::parse, record -> record.weight);
		}
		return streetSuffixDataFile;
	}

	public WeightedDataFile<ZipCodeDataFileRecord> getZipCodeDataFile()
	{
		if (zipCodeDataFile == null)
		{
			zipCodeDataFile = loadWeightedDataFile("ZipCode.txt", ZipCodeDataFileRecord::parse, record -> record.weight);
		}
		return zipCodeDataFile;
	}

	public WeightedDataFile<CompanySpRateDataFileRecord> getCompanySpRateDataFile()
	{
		if (companySpRateDataFile == null)
		{
			companySpRateDataFile = loadWeightedDataFile("CompanySPRate.txt", CompanySpRateDataFileRecord::parse, record -> record.weight);
		}
		return companySpRateDataFile;
	}

	public WeightedDataFile<NewsDataFileRecord> getNewsDataFile()
	{
		if (newsDataFile == null)
		{
			// The News.txt file uses LastName.txt as its source of "words"
			newsDataFile = loadWeightedDataFile("LastName.txt", NewsDataFileRecord::parse, record -> record.weight);
		}
		return newsDataFile;
	}

	public BucketedDataFile<TaxRateCountryDataFileRecord> getTaxRateCountryDataFile()
	{
		if (taxRateCountryDataFile == null)
		{
			taxRateCountryDataFile = loadBucketedDataFile("TaxRatesCountry.txt", TaxRateCountryDataFileRecord::parse);
		}
		return taxRateCountryDataFile;
	}

	public BucketedDataFile<TaxRateDivisionDataFileRecord> getTaxRateDivisionDataFile()
	{
		if (taxRateDivisionDataFile == null)
		{
			taxRateDivisionDataFile = loadBucketedDataFile("TaxRatesDivision.txt", TaxRateDivisionDataFileRecord::parse);
		}
		return taxRateDivisionDataFile;
	}

	public List<ChargeDataFileRecord> getChargeDataFile()
	{
		if (chargeDataFile == null)
		{
			chargeDataFile = loadDataFile("Charge.txt", ChargeDataFileRecord::parse);
		}
		return chargeDataFile;
	}

	public List<CommissionRateDataFileRecord> getCommissionRateDataFile()
	{
		if (commissionRateDataFile == null)
		{
			commissionRateDataFile = loadDataFile("CommissionRate.txt", CommissionRateDataFileRecord::parse);
		}
		return commissionRateDataFile;
	}

	public List<ExchangeDataFileRecord> getExchangeDataFile()
	{
		if (exchangeDataFile == null)
		{
			exchangeDataFile = loadDataFile("Exchange.txt", ExchangeDataFileRecord::parse);
		}
		return exchangeDataFile;
	}

	public List<IndustryDataFileRecord> getIndustryDataFile()
	{
		if (industryDataFile == null)
		{
			industryDataFile = loadDataFile("Industry.txt", IndustryDataFileRecord::parse);
		}
		return industryDataFile;
	}

	public List<SectorDataFileRecord> getSectorDataFile()
	{
		if (sectorDataFile == null)
		{
			sectorDataFile = loadDataFile("Sector.txt", SectorDataFileRecord::parse);
		}
		return sectorDataFile;
	}

	public CompanyFile getCompanyFile()
	{
		if (companyFile == null)
		{
			// The constructor requires the DataFileManager itself to access other files
			// and configuration parameters it might need.
			companyFile = new CompanyFile(this);
		}
		return companyFile;
	}

	public List<CompanyDataFileRecord> getCompanyDataFile()
	{
		if (companyDataFile == null)
		{
			companyDataFile = loadDataFile("Company.txt", CompanyDataFileRecord::parse);
		}
		return companyDataFile;
	}

	public List<StatusTypeDataFileRecord> getStatusTypeDataFile()
	{
		if (statusTypeDataFile == null)
		{
			statusTypeDataFile = loadDataFile(
					"StatusType.txt",
					StatusTypeDataFileRecord::parse);
		}
		return statusTypeDataFile;
	}

	public List<NonTaxableAccountNameDataFileRecord> getNonTaxableAccountNameDataFile()
	{
		if (nonTaxableAccountNameDataFile == null)
		{
			nonTaxableAccountNameDataFile = loadDataFile("NonTaxableAccountName.txt", NonTaxableAccountNameDataFileRecord::parse);
		}
		return nonTaxableAccountNameDataFile;
	}

	public List<TaxableAccountNameDataFileRecord> getTaxableAccountNameDataFile()
	{
		if (taxableAccountNameDataFile == null)
		{
			taxableAccountNameDataFile = loadDataFile("TaxableAccountName.txt", TaxableAccountNameDataFileRecord::parse);
		}
		return taxableAccountNameDataFile;
	}

	public List<CompanyCompetitorDataFileRecord> getCompanyCompetitorDataFile()
	{
		if (companyCompetitorDataFile == null)
		{
			companyCompetitorDataFile = loadDataFile("CompanyCompetitor.txt", CompanyCompetitorDataFileRecord::parse);
		}
		return companyCompetitorDataFile;
	}

	public SecurityFile getSecurityFile()
	{
		if (securityFile == null)
		{
			securityFile = new SecurityFile(this);
		}
		return securityFile;
	}

	public List<SecurityDataFileRecord> getSecurityDataFile()
	{
		if (securityDataFile == null)
		{
			securityDataFile = loadDataFile("Security.txt", SecurityDataFileRecord::parse);
		}
		return securityDataFile;
	}

	public TaxRateFile getTaxRateFile()
	{
		if (taxRateFile == null)
		{
			taxRateFile = new TaxRateFile(this);
		}
		return taxRateFile;
	}

	public List<TradeTypeDataFileRecord> getTradeTypeDataFile()
	{
		if (tradeTypeDataFile == null)
		{
			tradeTypeDataFile = loadDataFile("TradeType.txt", TradeTypeDataFileRecord::parse);
		}
		return tradeTypeDataFile;
	}
}