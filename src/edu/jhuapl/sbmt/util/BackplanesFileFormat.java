package edu.jhuapl.sbmt.util;

public enum BackplanesFileFormat
{
    FITS(new FitsBackplanesFile(), ".fit"), IMG(new ImgBackplanesFile(), ".img");

    private BackplanesFile file;
    private String extension;

    private BackplanesFileFormat(BackplanesFile file, String extension)
    {
        this.file = file;
        this.extension = extension;
    }

    public BackplanesFile getFile()
    {
        return file;
    }

    public String getExtension()
    {
        return extension;
    }
}