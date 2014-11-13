package com.linkedin.thirdeye.impl;

import com.linkedin.thirdeye.api.StarTreeConstants;
import com.linkedin.thirdeye.api.StarTreeRecord;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class StarTreeRecordImpl implements StarTreeRecord
{
  private final Map<String, String> dimensionValues;
  private final Map<String, Integer> metricValues;
  private final Long time;

  private AtomicReference<String> key;
  private AtomicReference<String> timeKey;

  public StarTreeRecordImpl(Map<String, String> dimensionValues, Map<String, Integer> metricValues, Long time)
  {
    this.dimensionValues = dimensionValues;
    this.metricValues = metricValues;
    this.time = time;
    this.key = new AtomicReference<String>();
    this.timeKey = new AtomicReference<String>();
  }

  @Override
  public Map<String, String> getDimensionValues()
  {
    return dimensionValues;
  }

  @Override
  public Map<String, Integer> getMetricValues()
  {
    return metricValues;
  }

  @Override
  public Long getTime()
  {
    return time;
  }

  @Override
  public String getKey(boolean includeTime)
  {
    if (includeTime)
    {
      if (timeKey.get() == null)
      {
        timeKey.compareAndSet(null, dimensionValues + "@" + time);
      }
      return timeKey.get();
    }
    else
    {
      if (key.get() == null)
      {
        key.compareAndSet(null, dimensionValues.toString());
      }
      return key.get();
    }
  }

  @Override
  public StarTreeRecord relax(String dimensionName)
  {
    return relax(Arrays.asList(dimensionName));
  }

  @Override
  public StarTreeRecord relax(Collection<String> dimensionNames)
  {
    Builder builder = new Builder();

    for (Map.Entry<String, String> entry : dimensionValues.entrySet())
    {
      if (dimensionNames.contains(entry.getKey()))
      {
        builder.setDimensionValue(entry.getKey(), StarTreeConstants.STAR);
      }
      else
      {
        builder.setDimensionValue(entry.getKey(), entry.getValue());
      }
    }

    for (Map.Entry<String, Integer> entry : metricValues.entrySet())
    {
      builder.setMetricValue(entry.getKey(), entry.getValue());
    }

    builder.setTime(time);

    return builder.build();
  }

  @Override
  public StarTreeRecord copy(boolean keepMetrics)
  {
    Builder builder = new Builder();

    for (Map.Entry<String, String> entry : dimensionValues.entrySet())
    {
      builder.setDimensionValue(entry.getKey(), entry.getValue());
    }

    if (keepMetrics)
    {
      for (Map.Entry<String, Integer> entry : metricValues.entrySet())
      {
        builder.setMetricValue(entry.getKey(), entry.getValue());
      }
    }

    builder.setTime(time);

    return builder.build();
  }

  @Override
  public StarTreeRecord aliasOther(String dimensionName)
  {
    return aliasOther(Arrays.asList(dimensionName));
  }

  @Override
  public StarTreeRecord aliasOther(Collection<String> otherDimensionNames)
  {
    Builder builder = new Builder();

    for (Map.Entry<String, String> entry : dimensionValues.entrySet())
    {
      if (otherDimensionNames.contains(entry.getKey()))
      {
        builder.setDimensionValue(entry.getKey(), StarTreeConstants.OTHER);
      }
      else
      {
        builder.setDimensionValue(entry.getKey(), entry.getValue());
      }
    }

    for (Map.Entry<String, Integer> entry : metricValues.entrySet())
    {
      builder.setMetricValue(entry.getKey(), entry.getValue());
    }

    builder.setTime(time);

    return builder.build();
  }

  @Override
  public int hashCode()
  {
    return dimensionValues.hashCode() + 13 * (time == null ? 1 : time.hashCode());
  }

  @Override
  public boolean equals(Object o)
  {
    if (!(o instanceof StarTreeRecord))
    {
      return false;
    }
    StarTreeRecord r = (StarTreeRecord) o;

    boolean dimensionEquals = dimensionValues == null ? r.getDimensionValues() == null : dimensionValues.equals(r.getDimensionValues());
    boolean timeEquals = time == null ? r.getTime() == null : time.equals(r.getTime());

    return dimensionEquals && timeEquals;
  }

  @Override
  public String toString()
  {
    return new StringBuilder()
            .append("dimensions=")
            .append(dimensionValues)
            .append(";metrics=")
            .append(metricValues)
            .append(";time=")
            .append(time)
            .toString();
  }

  public static class Builder
  {
    private final Map<String, String> dimensionValues = new HashMap<String, String>();
    private final Map<String, Integer> metricValues = new HashMap<String, Integer>();
    private Long time;

    public Map<String, String> getDimensionValues()
    {
      return dimensionValues;
    }

    public Map<String, Integer> getMetricValues()
    {
      return metricValues;
    }

    public Long getTime()
    {
      return time;
    }

    public Builder setDimensionValue(String dimensionName, String dimensionValue)
    {
      dimensionValues.put(dimensionName, dimensionValue);
      return this;
    }

    public Builder setDimensionValues(Map<String, String> dimensionValues)
    {
      this.dimensionValues.putAll(dimensionValues);
      return this;
    }

    public Builder updateDimensionValues(Map<String, String> dimensionValues)
    {
      for (Map.Entry<String, String> entry : dimensionValues.entrySet())
      {
        String current = this.dimensionValues.get(entry.getKey());
        if (current == null)
        {
          this.dimensionValues.put(entry.getKey(), entry.getValue());
        }
        else if (!current.equals(entry.getValue()))
        {
          this.dimensionValues.put(entry.getKey(), StarTreeConstants.STAR);
        }
      }
      return this;
    }

    public Builder setMetricValue(String metricName, Integer metricValue)
    {
      metricValues.put(metricName, metricValue);
      return this;
    }

    public Builder setMetricValues(Map<String, Integer> metricValues)
    {
      this.metricValues.putAll(metricValues);
      return this;
    }

    public Builder updateMetricValues(Map<String, Integer> metricValues)
    {
      for (Map.Entry<String, Integer> entry : metricValues.entrySet())
      {
        Integer current = this.metricValues.get(entry.getKey());
        this.metricValues.put(entry.getKey(), current + entry.getValue());
      }
      return this;
    }

    public Builder setTime(Long time)
    {
      this.time = time;
      return this;
    }

    public StarTreeRecord build()
    {
      return new StarTreeRecordImpl(dimensionValues, metricValues, time);
    }
  }
}
