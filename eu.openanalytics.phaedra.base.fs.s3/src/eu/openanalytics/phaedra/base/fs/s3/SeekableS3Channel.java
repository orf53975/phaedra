package eu.openanalytics.phaedra.base.fs.s3;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

public class SeekableS3Channel implements SeekableByteChannel, Closeable {

	private AmazonS3 s3;
	private String bucketName;
	private String key;

	private long length;
	
	private S3Object currentObject;
	private boolean open;
	private long pos;
	
	public SeekableS3Channel(AmazonS3 s3, String bucketName, String key) throws IOException {
		this.s3 = s3;
		this.bucketName = bucketName;
		this.key = key;
		
		this.length = s3.getObjectMetadata(bucketName, key).getContentLength();
		this.open = true;
		this.pos = -1;
//		seek(0, -1);
	}
	
	@Override
	public boolean isOpen() {
		return open;
	}

	@Override
	public void close() throws IOException {
		if (currentObject != null) currentObject.close();
		open = false;
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		int requestedRead = dst.remaining();
		if (requestedRead < 1) return requestedRead;
		
		if (pos == -1) seek(0, requestedRead);
		
		byte[] bytes = new byte[requestedRead];
		
		int totalRead = 0;
		while (totalRead < requestedRead) {
			int read = currentObject.getObjectContent().read(bytes, totalRead, bytes.length - totalRead);
			if (read == -1) break;
			totalRead += read;
		}
		
		dst.put(bytes, 0, totalRead);
		return totalRead;
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		throw new IOException("S3 writes are not supported");
	}

	@Override
	public long position() throws IOException {
		return pos;
	}

	@Override
	public SeekableByteChannel position(long newPosition) throws IOException {
		seek(newPosition, -1);
		return this;
	}

	public SeekableByteChannel position(long newPosition, long expectedRead) throws IOException {
		seek(newPosition, expectedRead);
		return this;
	}
	
	@Override
	public long size() throws IOException {
		return length;
	}

	@Override
	public SeekableByteChannel truncate(long size) throws IOException {
		throw new IOException("S3 writes are not supported");
	}
	
	private void seek(long newPos, long expectedRead) throws IOException {
		if (pos == newPos && expectedRead == 0) return;
		this.pos = newPos;
		GetObjectRequest req = new GetObjectRequest(bucketName, key);
		if (expectedRead == -1) req.setRange(pos);
		else req.setRange(pos, pos + expectedRead - 1);
		if (currentObject != null) currentObject.close();
		this.currentObject = s3.getObject(req);
	}
}
