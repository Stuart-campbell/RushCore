package co.uk.rushorm.core;

import java.util.List;
import java.util.Map;

/**
 * Created by Stuart on 10/12/14.
 */
public interface RushUpgradeManager {

    public interface UpgradeCallback {
        public RushStatementRunner.ValuesCallback runStatement(String sql);
        public void runRaw(String sql);
        public void createClasses(List<Class<? extends Rush>> missingClasses);
    }

    public void upgrade(List<Class<? extends Rush>> classList, UpgradeCallback callback, Map<Class<? extends Rush>, AnnotationCache> annotationCache);

}
