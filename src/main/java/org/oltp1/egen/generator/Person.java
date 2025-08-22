package org.oltp1.egen.generator;

import java.util.function.Function;

import org.oltp1.egen.io.DataFileManager;
import org.oltp1.egen.io.WeightedDataFile;
import org.oltp1.egen.model.CustomerRow;
import org.oltp1.egen.util.TpcRandom;

/**
 * Generates personal data for customers (names, gender, tax ID). This is a
 * direct port of the CPerson class. It is CRITICAL that the seeding logic is
 * stateless between calls for different attributes to ensure deterministic and
 * correct output. The RNG state is saved and restored in each public method.
 * Based on inc/Person.h and src/Person.cpp
 */
public class Person
{
	private static final long IDENT_T_SHIFT = 4300000000L;
	private static final int LOAD_UNIT_SIZE = 1000;
	private static final int TAX_ID_FMT_LEN = 14;

	// RNG Seeds from RNGSeeds.h
	private static final long RNG_SEED_BASE_LAST_NAME = 35846049L;
	private static final long RNG_SEED_BASE_FIRST_NAME = 95066470L;
	private static final long RNG_SEED_BASE_MIDDLE_INITIAL = 71434514L;
	private static final long RNG_SEED_BASE_GENDER = 9568922L;
	private static final long RNG_SEED_BASE_TAX_ID = 8731255L;

	private final DataFileManager dfm;
	private final TpcRandom random; // Passed from the table generator

	// Caching logic
	private final boolean cacheEnabled;
	private final int cacheSize;
	private long cacheOffset;
	private final int[] firstNameCache;
	private final int[] lastNameCache;
	private final char[] genderCache;
	private static final int INVALID_NAME_CACHE_ENTRY = -1;
	private static final char INVALID_GENDER_CACHE_ENTRY = 'X';

	public Person(DataFileManager dfm, long startFromCustomer, boolean cacheEnabled)
	{
		this.dfm = dfm;
		this.random = new TpcRandom(0);
		this.cacheEnabled = cacheEnabled;

		if (cacheEnabled)
		{
			this.cacheSize = LOAD_UNIT_SIZE;
			this.cacheOffset = startFromCustomer + IDENT_T_SHIFT;
			this.firstNameCache = new int[cacheSize];
			this.lastNameCache = new int[cacheSize];
			this.genderCache = new char[cacheSize];
			initNextLoadUnit(0);
		}
		else
		{
			this.cacheSize = 0;
			this.cacheOffset = 0;
			this.firstNameCache = null;
			this.lastNameCache = null;
			this.genderCache = null;
		}
	}

	public void initNextLoadUnit()
	{
		initNextLoadUnit(LOAD_UNIT_SIZE);
	}

	protected void initNextLoadUnit(long offsetIncrement)
	{
		if (cacheEnabled)
		{
			this.cacheOffset += offsetIncrement;
			for (int i = 0; i < cacheSize; i++)
			{
				firstNameCache[i] = INVALID_NAME_CACHE_ENTRY;
				lastNameCache[i] = INVALID_NAME_CACHE_ENTRY;
				genderCache[i] = INVALID_GENDER_CACHE_ENTRY;
			}
		}
	}

	/**
	 * Populates the name, gender, and tax ID fields of a CustomerRow object.
	 */
	public void populate(CustomerRow row)
	{
		row.c_gndr = getGender(row.c_id);
		row.c_l_name = getLastName(row.c_id);
		row.c_f_name = getFirstName(row.c_id, row.c_gndr == 'M');
		row.c_m_name = String.valueOf(getMiddleName(row.c_id));
		row.c_tax_id = getTaxId(row.c_id);
	}

	/**
	 * Generic method to get a name from a weighted data file.
	 * 
	 * @param customerId
	 *            The customer ID to generate the name for.
	 * @param weightedDataFile
	 *            The WeightedDataFile instance containing names.
	 * @param nameExtractor
	 *            A function to extract the name string from a record.
	 * @param cache
	 *            The cache to use for this name type.
	 * @param baseSeed
	 *            The base RNG seed for this name type.
	 * @return The deterministically generated name.
	 */
	private <T> String getName(long customerId, WeightedDataFile<T> weightedDataFile, Function<T, String> nameExtractor, int[] cache, long baseSeed)
	{
		int indexInCache = (int) (customerId - cacheOffset);
		if (cacheEnabled && indexInCache >= 0 && indexInCache < cacheSize && cache[indexInCache] != INVALID_NAME_CACHE_ENTRY)
		{
			return nameExtractor.apply(weightedDataFile.getRecord(cache[indexInCache]));
		}

		long oldSeed = random.getSeed();
		random.setSeed(TpcRandom.rndNthElement(baseSeed, customerId));
		int threshold = random.rndIntRange(0, weightedDataFile.size() - 1);
		random.setSeed(oldSeed);

		if (cacheEnabled && indexInCache >= 0 && indexInCache < cacheSize)
		{
			cache[indexInCache] = threshold;
		}

		return nameExtractor.apply(weightedDataFile.getRecord(threshold));
	}

	public String getFirstName(long customerId)
	{
		boolean isMale = isMaleGender(customerId);
		return getFirstName(customerId, isMale);
	}

	public String getFirstName(long customerId, boolean isMale)
	{
		if (isMale)
		{
			return getName(customerId, dfm.getMaleFirstNameDataFile(), record -> record.name, firstNameCache, RNG_SEED_BASE_FIRST_NAME);
		}
		else
		{
			return getName(customerId, dfm.getFemaleFirstNameDataFile(), record -> record.name, firstNameCache, RNG_SEED_BASE_FIRST_NAME);
		}
	}

	public String getLastName(long customerId)
	{
		return getName(customerId, dfm.getLastNameDataFile(), record -> record.name, lastNameCache, RNG_SEED_BASE_LAST_NAME);
	}

	public char getMiddleName(long customerId)
	{
		long oldSeed = random.getSeed();
		random.setSeed(TpcRandom.rndNthElement(RNG_SEED_BASE_MIDDLE_INITIAL, customerId));
		char middleInitial = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".charAt(random.rndIntRange(0, 25));
		random.setSeed(oldSeed);
		return middleInitial;
	}

	private char getGender(long customerId)
	{
		int indexInCache = (int) (customerId - cacheOffset);
		if (cacheEnabled && indexInCache >= 0 && indexInCache < cacheSize && genderCache[indexInCache] != INVALID_GENDER_CACHE_ENTRY)
		{
			return genderCache[indexInCache];
		}

		long oldSeed = random.getSeed();
		random.setSeed(TpcRandom.rndNthElement(RNG_SEED_BASE_GENDER, customerId));
		char gender = (random.rndIntRange(1, 100) <= 49) ? 'M' : 'F';
		random.setSeed(oldSeed);

		if (cacheEnabled && indexInCache >= 0 && indexInCache < cacheSize)
		{
			genderCache[indexInCache] = gender;
		}
		return gender;
	}

	public String getTaxId(long customerId)
	{
		long oldSeed = random.getSeed();
		random.setSeed(TpcRandom.rndNthElement(RNG_SEED_BASE_TAX_ID, customerId * TAX_ID_FMT_LEN));
		String taxId = random.rndAlphaNumFormatted("nnnaannnnaannn");
		random.setSeed(oldSeed);
		return taxId;
	}

	/**
	 * Returns true if the customer's gender is male. This is a convenience method
	 * that calls getGender(). The logic is a direct port of the C++ IsMaleGender
	 * method.
	 *
	 * @param customerId
	 *            The customer's unique ID.
	 * @return true if the customer is male, false otherwise.
	 */
	public boolean isMaleGender(long customerId)
	{
		return getGender(customerId) == 'M';
	}
}