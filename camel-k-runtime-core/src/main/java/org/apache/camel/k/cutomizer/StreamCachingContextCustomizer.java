package org.apache.camel.k.cutomizer;

import org.apache.camel.CamelContext;
import org.apache.camel.k.ContextCustomizer;
import org.apache.camel.k.Runtime;
import org.apache.camel.spi.StreamCachingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamCachingContextCustomizer implements ContextCustomizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamCachingContextCustomizer.class);

    private boolean enabled;
    private boolean anySpoolRules;
    private int bufferSize;
    private boolean removeSpoolDirectoryWhenStopping;
    private String spoolChiper;
    private String spoolDirectory;
    private long spoolThreshold;
    private String spoolUsedHeapMemoryLimit;
    private int spoolUsedHeapMemoryThreshold;

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public String getSpoolChiper() {
        return spoolChiper;
    }

    public void setSpoolChiper(String spoolChiper) {
        this.spoolChiper = spoolChiper;
    }

    public String getSpoolDirectory() {
        return spoolDirectory;
    }

    public void setSpoolDirectory(String spoolDirectory) {
        this.spoolDirectory = spoolDirectory;
    }

    public long getSpoolThreshold() {
        return spoolThreshold;
    }

    public void setSpoolThreshold(long spoolThreshold) {
        this.spoolThreshold = spoolThreshold;
    }

    public String getSpoolUsedHeapMemoryLimit() {
        return spoolUsedHeapMemoryLimit;
    }

    public void setSpoolUsedHeapMemoryLimit(String spoolUsedHeapMemoryLimit) {
        this.spoolUsedHeapMemoryLimit = spoolUsedHeapMemoryLimit;
    }

    public int getSpoolUsedHeapMemoryThreshold() {
        return spoolUsedHeapMemoryThreshold;
    }

    public void setSpoolUsedHeapMemoryThreshold(int spoolUsedHeapMemoryThreshold) {
        this.spoolUsedHeapMemoryThreshold = spoolUsedHeapMemoryThreshold;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAnySpoolRules() {
        return anySpoolRules;
    }

    public void setAnySpoolRules(boolean anySpoolRules) {
        this.anySpoolRules = anySpoolRules;
    }

    public boolean isRemoveSpoolDirectoryWhenStopping() {
        return removeSpoolDirectoryWhenStopping;
    }

    public void setRemoveSpoolDirectoryWhenStopping(boolean removeSpoolDirectoryWhenStopping) {
        this.removeSpoolDirectoryWhenStopping = removeSpoolDirectoryWhenStopping;
    }

    @Override
    public void apply(CamelContext camelContext, Runtime.Registry runtimeRegistry) {
        camelContext.setStreamCaching(isEnabled());
        camelContext.getStreamCachingStrategy().setAnySpoolRules(isAnySpoolRules());
        camelContext.getStreamCachingStrategy().setBufferSize(getBufferSize());
        camelContext.getStreamCachingStrategy().setRemoveSpoolDirectoryWhenStopping(isRemoveSpoolDirectoryWhenStopping());
        camelContext.getStreamCachingStrategy().setSpoolChiper(getSpoolChiper());
        if (getSpoolDirectory() != null) {
            camelContext.getStreamCachingStrategy().setSpoolDirectory(getSpoolDirectory());
        }
        if (getSpoolThreshold() != 0) {
            camelContext.getStreamCachingStrategy().setSpoolThreshold(getSpoolThreshold());
        }
        if (getSpoolUsedHeapMemoryLimit() != null) {
            StreamCachingStrategy.SpoolUsedHeapMemoryLimit limit;
            if ("Committed".equalsIgnoreCase(getSpoolUsedHeapMemoryLimit())) {
                limit = StreamCachingStrategy.SpoolUsedHeapMemoryLimit.Committed;
            } else if ("Max".equalsIgnoreCase(getSpoolUsedHeapMemoryLimit())) {
                limit = StreamCachingStrategy.SpoolUsedHeapMemoryLimit.Max;
            } else {
                throw new IllegalArgumentException("Invalid option " + getSpoolUsedHeapMemoryLimit() + " must either be Committed or Max");
            }
            camelContext.getStreamCachingStrategy().setSpoolUsedHeapMemoryLimit(limit);
        }
        if (getSpoolUsedHeapMemoryThreshold() != 0) {
            camelContext.getStreamCachingStrategy().setSpoolUsedHeapMemoryThreshold(getSpoolUsedHeapMemoryThreshold());
        }
        LOGGER.info("Configured camel context through CamelContextCustomizer.class");
    }
}
