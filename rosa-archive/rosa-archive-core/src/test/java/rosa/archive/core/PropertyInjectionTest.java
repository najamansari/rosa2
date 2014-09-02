package rosa.archive.core;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import rosa.archive.core.GuiceJUnitRunner.GuiceModules;
import rosa.archive.core.config.AppContext;

import java.util.Arrays;

@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ArchiveCoreModule.class})
public class PropertyInjectionTest {

    @Inject
    private AppContext context;

    @Test
    public void test() {
        System.out.println(context.CHARSET);
        System.out.println(Arrays.toString(context.languages()));
    }

}
