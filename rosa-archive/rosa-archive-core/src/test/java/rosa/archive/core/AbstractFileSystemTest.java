package rosa.archive.core;

import org.junit.Before;
import org.junit.runner.RunWith;

import rosa.archive.core.GuiceJUnitRunner.GuiceModules;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertNotNull;

/**
 *
 */
@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ArchiveCoreModule.class})
public abstract class AbstractFileSystemTest {

    protected ByteStreamGroup base;

    @Before
    public void setup() throws URISyntaxException {
        URL u = getClass().getClassLoader().getResource("data/character_names.csv");
        assertNotNull(u);

        Path path = Paths.get(u.toURI()).getParent().getParent();        
        assertNotNull(path);

        base = new FSByteStreamGroup(path.toString());
    }

}