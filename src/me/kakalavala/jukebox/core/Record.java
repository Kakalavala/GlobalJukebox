package me.kakalavala.jukebox.core;

public enum Record {
	
	DISC_13(774),
	CAT(775),
	BLOCKS(776),
	CHIRP(777),
	FAR(778),
	MALL(779),
	MELLOHI(780),
	STAL(781),
	STRAD(782),
	WARD(783),
	DISC_11(784),
	WAIT(785),
	STOP(0)
	;
	
	int id;
	
	Record(int id) {
		this.id = id;
	}

}
