package net.leberfinger.osm.categories;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;

/**
 * Read TSV files provided by libpostal
 */
public class CategoryImport {

	private Categories categories = new Categories();
	
	public enum TSVHeader
	{
		key, value, is_plural, phrase;
	}
	
	public CategoryImport()
	{
	}
	
	public Categories getCategories()
	{
		return categories;
	}
	
	public void importBundledFiles() throws IOException
	{
		Set<String> categoryFiles = listBundledFiles();
		for(String f : categoryFiles)
		{
			String lang = getLang(Paths.get(f));
			
			try(InputStream in = getClass().getClassLoader().getResourceAsStream(f))
			{
				readTSV(lang, in);
			}
		}
	}
	
	public void readTSV(final String language, InputStream in) throws IOException
	{
		CSVFormat tsvFormat = CSVFormat.TDF.withHeader(TSVHeader.class).withFirstRecordAsHeader();
		try(Reader r = new InputStreamReader(in, StandardCharsets.UTF_8))
		{
			CSVParser records = tsvFormat.parse(r);
			for(CSVRecord record : records)
			{
				String osmKey = record.get(TSVHeader.key);
				String osmValue = record.get(TSVHeader.value);
//				boolean isPlural = "1".equals(record.get(TSVHeader.is_plural));
				String phrase = record.get(TSVHeader.phrase);
				
				categories.add(osmKey, osmValue, language, phrase);
			}
		}
	}
	
	protected static Set<String> listBundledFiles()
	{
		String packageName = CategoryImport.class.getPackage().getName();
		Reflections reflections = new Reflections(packageName, new ResourcesScanner());
		return reflections.getResources(Pattern.compile(".*\\.tsv"));
	}

	protected Set<Path> listFiles(File folder) throws IOException {
		try (Stream<Path> files = Files.list(folder.toPath())) {
			return files.filter(f -> isTSVFile(f)).map(f -> f.toAbsolutePath()).collect(Collectors.toSet());
		}
	}

	protected static boolean isTSVFile(Path f) {
		final String fileName = f.getFileName().toString();
		return fileName.toLowerCase().endsWith(".tsv");
	}

	protected static String getLang(Path f) {
		final String fileName = f.getFileName().toString();
		return fileName.toLowerCase().substring(0, 2);
	}
}
