package edu.jhuapl.sbmt.util;

public abstract class BasicFileWriter implements FileWriter
{
    protected final String filename;

    public BasicFileWriter(String filename)
    {
        this.filename=filename;
    }

    @Override
    public String getFileName()
    {
        return filename;
    }
}
