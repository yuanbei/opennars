package nars.io;

/** A low-level handler for parsing text.
 *  Named reaction because it is analogous to an automatic reflex or reaction. */
public interface TextReaction extends ObjectReaction<String> {
    
    /**
     * 
     * @param nar reasoner
     * @param input text to input
     * @return either:
     *      True meaning the input was handled and handle no others, 
     *      Task instance, 
     *      a new input task, or null
     */
    @Override
    public Object react(String input);
}
