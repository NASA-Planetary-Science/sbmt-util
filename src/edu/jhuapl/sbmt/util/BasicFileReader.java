package edu.jhuapl.sbmt.util;

import java.io.IOException;

public abstract class BasicFileReader<E extends IOException> implements FileReader<E>
{
    protected final String filename;

    public BasicFileReader(String filename)
    {
        this.filename=filename;
    }

    @Override
    public String getFileName()
    {
        return filename;
    }
}
