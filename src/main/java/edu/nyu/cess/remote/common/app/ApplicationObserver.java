package edu.nyu.cess.remote.common.app;

/**
 * Application State Observer Interface
 */
public interface ApplicationObserver
{
	void applicationUpdate(State applicationState);
}
