package org.oltp1.runner.sandbox;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.Random;
import java.util.UUID;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.OffHeapStore;

public class MVStoreReadBenchmark
{
	private static final int ENTRY_COUNT = 5_000_000;
	private static final int WARMUP_ITERATIONS = 10_000;
	private static final int BENCHMARK_ITERATIONS = 500_000;

	private MVStore store;
	private MVMap<Integer, Person> map;
	private Random random;
	private int[] randomKeys;

	/**
	 * POJO class representing a Person entit must be Serializable for MVStore to
	 * persist it
	 */
	public static class Person implements Serializable
	{
		private static final long serialVersionUID = 1L;

		private String id;
		private String firstName;
		private String lastName;
		private int age;
		private String email;
		private String address;
		private String city;
		private String country;
		private String phoneNumber;
		private double salary;
		private String department;
		private long timestamp;
		private String description;

		public Person()
		{
			// Default constructor needed for serialization
		}

		public Person(String id, String firstName, String lastName, int age,
				String email, String address, String city, String country,
				String phoneNumber, double salary, String department,
				long timestamp, String description)
		{
			this.id = id;
			this.firstName = firstName;
			this.lastName = lastName;
			this.age = age;
			this.email = email;
			this.address = address;
			this.city = city;
			this.country = country;
			this.phoneNumber = phoneNumber;
			this.salary = salary;
			this.department = department;
			this.timestamp = timestamp;
			this.description = description;
		}

		// Getters and setters
		public String getId()
		{
			return id;
		}

		public void setId(String id)
		{
			this.id = id;
		}

		public String getFirstName()
		{
			return firstName;
		}

		public void setFirstName(String firstName)
		{
			this.firstName = firstName;
		}

		public String getLastName()
		{
			return lastName;
		}

		public void setLastName(String lastName)
		{
			this.lastName = lastName;
		}

		public int getAge()
		{
			return age;
		}

		public void setAge(int age)
		{
			this.age = age;
		}

		public String getEmail()
		{
			return email;
		}

		public void setEmail(String email)
		{
			this.email = email;
		}

		public String getAddress()
		{
			return address;
		}

		public void setAddress(String address)
		{
			this.address = address;
		}

		public String getCity()
		{
			return city;
		}

		public void setCity(String city)
		{
			this.city = city;
		}

		public String getCountry()
		{
			return country;
		}

		public void setCountry(String country)
		{
			this.country = country;
		}

		public String getPhoneNumber()
		{
			return phoneNumber;
		}

		public void setPhoneNumber(String phoneNumber)
		{
			this.phoneNumber = phoneNumber;
		}

		public double getSalary()
		{
			return salary;
		}

		public void setSalary(double salary)
		{
			this.salary = salary;
		}

		public String getDepartment()
		{
			return department;
		}

		public void setDepartment(String department)
		{
			this.department = department;
		}

		public long getTimestamp()
		{
			return timestamp;
		}

		public void setTimestamp(long timestamp)
		{
			this.timestamp = timestamp;
		}

		public String getDescription()
		{
			return description;
		}

		public void setDescription(String description)
		{
			this.description = description;
		}

		@Override
		public String toString()
		{
			return "Person{id='" + id + "', name='" + firstName + " " + lastName +
					"', age=" + age + ", department='" + department + "'}";
		}
	}

	public static void main(String[] args)
	{
		MVStoreReadBenchmark benchmark = new MVStoreReadBenchmark();

		try
		{
			System.out.println("MVStore Read Performance Benchmark (POJO Version)");
			System.out.println("==================================================");
			System.out.println("Configuration:");
			System.out.println("  Entries: " + ENTRY_COUNT);
			System.out.println("  Value type: Person POJO");
			System.out.println("  Warmup iterations: " + WARMUP_ITERATIONS);
			System.out.println("  Benchmark iterations: " + BENCHMARK_ITERATIONS);
			System.out.println();

			// Initialize and populate the store
			benchmark.initialize();
			benchmark.populateStore();

			// Run warmup
			System.out.println("Running warmup...");
			benchmark.warmup();

			// Run the actual benchmark
			System.out.println("\nRunning benchmark...");
			benchmark.runBenchmark();

			// Print memory statistics
			benchmark.printStatistics();

		}
		finally
		{
			benchmark.cleanup();
		}
	}

	private void initialize()
	{
		System.out.println("Initializing MVStore with off-heap storage...");

		// Create MVStore with off-heap storage

		OffHeapStore offHeap = new OffHeapStore();
		store = new MVStore.Builder().fileStore(offHeap).open();

		// Open a map that stores Person objects
		map = store.openMap("people");
		random = new Random(42); // Fixed seed for reproducibility

		// Pre-generate random keys for consistent benchmark
		randomKeys = new int[BENCHMARK_ITERATIONS];
		for (int i = 0; i < BENCHMARK_ITERATIONS; i++)
		{
			randomKeys[i] = random.nextInt(ENTRY_COUNT);
		}
	}

	private void populateStore()
	{
		System.out.println("Populating store with " + ENTRY_COUNT + " Person objects...");

		long startTime = System.nanoTime();

		String[] departments = { "Engineering", "Sales", "Marketing", "HR", "Finance", "Operations" };
		String[] cities = { "New York", "London", "Tokyo", "Paris", "Berlin", "Sydney", "Toronto", "Mumbai" };
		String[] countries = { "USA", "UK", "Japan", "France", "Germany", "Australia", "Canada", "India" };

		for (int i = 0; i < ENTRY_COUNT; i++)
		{
			Person person = createPerson(i, departments, cities, countries);
			map.put(i, person);

			if (i % 10000 == 0)
			{
				System.out.print(".");
				store.commit(); // Periodic commits
			}
		}

		store.commit();
		store.compactFile(60000); //

		long endTime = System.nanoTime();
		double seconds = (endTime - startTime) / 1_000_000_000.0;

		System.out.println("\nPopulation completed in " + String.format("%.2f", seconds) + " seconds");
		System.out.println("Store size: " + map.size() + " entries");
	}

	private Person createPerson(int index, String[] departments, String[] cities, String[] countries)
	{
		String id = UUID.randomUUID().toString();
		String firstName = "FirstName" + index;
		String lastName = "LastName" + index;
		int age = 20 + random.nextInt(50);
		String email = "person" + index + "@example.com";
		String address = index + " Main Street, Apt " + random.nextInt(100);
		String city = cities[random.nextInt(cities.length)];
		String country = countries[random.nextInt(countries.length)];
		String phoneNumber = String
				.format(
						"+1-%03d-%03d-%04d",
						random.nextInt(1000),
						random.nextInt(1000),
						random.nextInt(10000));
		double salary = 30000 + random.nextDouble() * 150000;
		String department = departments[random.nextInt(departments.length)];
		long timestamp = System.currentTimeMillis();
		String description = "This is a description for person " + index +
				". They work in " + department +
				" and have been with the company for " + random.nextInt(10) + " years.";

		return new Person(
				id,
				firstName,
				lastName,
				age,
				email,
				address,
				city,
				country,
				phoneNumber,
				salary,
				department,
				timestamp,
				description);
	}

	private void warmup()
	{

		try (Writer w = Writer.nullWriter())
		{
			// Warmup to ensure JIT compilation and cache stabilization
			for (int i = 0; i < WARMUP_ITERATIONS; i++)
			{
				int key = random.nextInt(ENTRY_COUNT);
				Person person = map.get(key);
				if (person == null)
				{
					System.err.printf("Warning: null value for key: %s%n", key);
				}

				// Access some fields to ensure full deserialization
				w.write(person.getFirstName());
				w.write(person.getLastName());
			}

		}
		catch (IOException ioe)
		{
			throw new RuntimeException(ioe);
		}
	}

	private void runBenchmark()
	{
		// Benchmark random reads
		long totalStartTime = System.nanoTime();

		try (Writer w = Writer.nullWriter())
		{
			for (int i = 0; i < BENCHMARK_ITERATIONS; i++)
			{
				int key = randomKeys[i];

				Person person = map.get(key);

				if (person == null)
				{
					System.err.printf("Warning: null value for key: %s%n", key);
				}
				else
				{
					// Access some fields to ensure full deserialization
					w.write(person.getId());
					w.write(person.getAge());
				}
			}
		}
		catch (IOException ioe)
		{
			throw new RuntimeException(ioe);
		}

		long totalEndTime = System.nanoTime();

		// Calculate statistics
		long totalTime = totalEndTime - totalStartTime;
		double totalSeconds = totalTime / 1_000_000_000.0;
		double throughput = BENCHMARK_ITERATIONS / totalSeconds;
		double avgLatencyNs = (double) totalTime / BENCHMARK_ITERATIONS;
		double avgLatencyUs = avgLatencyNs / 1000;

		// Print results
		System.out.println("\nBenchmark Results:");
		System.out.println("==================");
		System.out.println("Total operations: " + BENCHMARK_ITERATIONS);
		System.out.println("Total time: " + String.format("%.3f", totalSeconds) + " seconds");
		System.out.println("Throughput: " + String.format("%.0f", throughput) + " ops/sec");
		System.out.println("\nLatency Statistics:");
		System.out.println("  Average: " + String.format("%.2f", avgLatencyUs) + " μs");

		// Sample a few retrieved objects to verify data integrity
		System.out.println("\nSample retrieved objects:");
		for (int i = 0; i < 3; i++)
		{
			int key = randomKeys[i];
			Person person = map.get(key);
			if (person != null)
			{
				System.out.println("  Key " + key + ": " + person);
			}
		}
	}

	private void printStatistics()
	{
		System.out.println("\nStore Statistics:");
		System.out.println("=================");
		System.out.println("Map size: " + map.size() + " entries");
		System.out.println("Store file size: " + store.getFileStore().size() + " bytes");
		System.out.println("Cache size: " + store.getCacheSize() + " KB");
		System.out.println("Cache used: " + store.getCacheSizeUsed() + " KB");
		System.out.println("Unsaved memory: " + store.getUnsavedMemory() + " bytes");

		// Calculate approximate object size
		long storeSize = store.getFileStore().size();
		double avgObjectSize = (double) storeSize / ENTRY_COUNT;
		System.out
				.println(
						"Approximate size per Person object: " +
								String.format("%.2f", avgObjectSize) + " bytes");

		// Memory statistics
		Runtime runtime = Runtime.getRuntime();
		long usedMemory = runtime.totalMemory() - runtime.freeMemory();
		System.out.println("\nJVM Memory:");
		System.out.println("  Used: " + (usedMemory / 1024 / 1024) + " MB");
		System.out.println("  Total: " + (runtime.totalMemory() / 1024 / 1024) + " MB");
		System.out.println("  Max: " + (runtime.maxMemory() / 1024 / 1024) + " MB");
	}

	private void cleanup()
	{
		System.out.println("\nCleaning up...");
		if (store != null)
		{
			store.close();
		}

		// Optionally delete the benchmark file
		java.io.File file = new java.io.File("benchmark.mv.db");
		if (file.exists())
		{
			file.delete();
			System.out.println("Benchmark file deleted.");
		}
	}
}
