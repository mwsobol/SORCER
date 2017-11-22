/*
 * Distribution Statement
 *
 * This computer software has been developed under sponsorship of the United States Air Force Research Lab. Any further
 * distribution or use by anyone or any data contained therein, unless otherwise specifically provided for,
 * is prohibited without the written approval of AFRL/RQVC-MSTC, 2210 8th Street Bldg 146, Room 218, WPAFB, OH  45433
 *
 * Disclaimer
 *
 * This material was prepared as an account of work sponsored by an agency of the United States Government. Neither
 * the United States Government nor the United States Air Force, nor any of their employees, makes any warranty,
 * express or implied, or assumes any legal liability or responsibility for the accuracy, completeness, or usefulness
 * of any information, apparatus, product, or process disclosed, or represents that its use would not infringe privately
 * owned rights.
 */
package sorcer.scratch;

import sorcer.service.Context;

import java.io.File;
import java.net.URL;

/**
 * Defines the semantics to produce scratch data and scratch URLs
 */
public interface ScratchManager {

    /**
     * Return a scratch directory.
     *
     * @return A File for use as a scratch directory.
     */
    File getScratchDir();

    /**
     * Return a scratch directory with the suffix
     *
     * @param suffix The directory key to append
     *
     * @return A File for use as a scratch directory.
     */
    File getScratchDir(String suffix);

    /**
     * Based on a {@link sorcer.service.Context}, return a scratch directory.
     *
     * @param context The Context, must not be null.
     * @param suffix The directory key to append
     *
     * @return A File for use as a scratch directory.
     */
    File getScratchDir(Context context, String suffix);

    /**
     * Given a File, return an accessible URL
     *
     * @param scratchFile The file to use, the file not exist and be acccessible.
     *
     * @return a URL that can be used to access the file remotely.
     */
    URL getScratchURL(File scratchFile);
}
