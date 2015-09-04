package nars.nal.nal3;


import nars.NARSeed;
import nars.nal.JavaNALTest;
import nars.nar.Classic;
import nars.nar.Default;
import nars.nar.DefaultMicro;
import nars.nar.NewDefault;
import nars.narsese.InvalidInputException;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

public class NAL3Test extends JavaNALTest {

    public static final int cycles = 500;

    public NAL3Test(NARSeed b) { super(b); }

    @Parameterized.Parameters(name= "{0}")
    public static Collection configurations() {
        return Arrays.asList(new Object[][]{
                {new Default()}, //NAL8 + NAL9 didnt solve it
                {new Default().level(3)}, //needs 3 for sets
                {new Default().setInternalExperience(null)},
                {new NewDefault()},
                {new NewDefault().setInternalExperience(null)},
                {new DefaultMicro() },
                {new Classic()}

                //{new Neuromorphic(4)},
        });
    }


    @Test
    public void compound_composition_two_premises() throws InvalidInputException {
        n.believe("<swan --> swimmer>",0.9f,0.9f).en("Swan is a type of swimmer.");
        n.believe("<swan --> bird>", 0.8f, 0.9f).en("Swan is a type of bird.");
        n.mustBelieve(cycles, "<swan --> (|,bird,swimmer)>", 0.98f, 0.81f).en("Swan is a type of bird or a type of swimmer.");
        n.mustBelieve(cycles, "<swan --> (&,bird,swimmer)>",0.72f,0.81f).en("Swan is a type of swimming bird.");
        n.run();
    }

    @Test
    public void compound_composition_two_premises2() throws InvalidInputException {
        n.believe("<sport --> competition>",0.9f,0.9f).en("Sport is a type of competition.");
        n.believe("<chess --> competition>", 0.8f, 0.9f).en("Chess is a type of competition.");
        n.mustBelieve(cycles, "<(|,chess,sport) --> competition>", 0.72f ,0.81f).en("If something is either chess or sport, then it is a competition.");
        n.mustBelieve(cycles, "<(&,chess,sport) --> competition>", 0.98f, 0.81f).en("If something is both chess and sport, then it is a competition.");
        n.run();
    }

    @Test
    public void compound_decomposition_two_premises() throws InvalidInputException {
        n.believe("<robin --> (|,bird,swimmer)>",1.0f,0.9f).en("Robin is a type of bird or a type of swimmer.");
        n.believe("<robin --> swimmer>", 0.0f, 0.9f).en("Robin is not a type of swimmer.");
        n.mustBelieve(cycles, "<robin --> bird>", 1.0f ,0.81f).en("Robin is a type of bird.");
        n.run();
    }

    @Test
    public void compound_decomposition_two_premises2() throws InvalidInputException {
        n.believe("<robin --> swimmer>",0.0f,0.9f).en("Robin is not a type of swimmer.");
        n.believe("<robin --> (-,mammal,swimmer)>", 0.0f, 0.9f).en("Robin is not a nonswimming mammal.");
        n.mustBelieve(cycles, "<robin --> mammal>", 0.0f ,0.81f).en("Robin is not a type of mammal.");
        n.run();
    }

    @Test
    public void set_operations() throws InvalidInputException {
        n.believe("<planetX --> {Mars,Pluto,Venus}>",1.0f,0.9f).en("PlanetX is Mars, Pluto, or Venus.");
        n.believe("<planetX --> {Pluto,Saturn}>", 1.0f, 0.7f).en("PlanetX is probably Pluto or Saturn.");
        n.mustBelieve(cycles, "<planetX --> {Mars,Pluto,Saturn,Venus}>", 0.97f ,0.81f).en("PlanetX is Mars, Pluto, Saturn, or Venus.");
        n.mustBelieve(cycles, "<planetX --> {Pluto}>", 0.63f ,0.81f).en("PlanetX is probably Pluto.");
        n.run();
    }

    @Test
    public void set_operations2() throws InvalidInputException {
        n.believe("<planetX --> {Mars,Pluto,Venus}>",1.0f,0.9f).en("PlanetX is Mars, Pluto, or Venus.");
        n.believe("<planetX --> {Pluto,Saturn}>", 0.1f, 0.9f).en("PlanetX is probably neither Pluto nor Saturn.");
        n.mustBelieve(cycles, "<planetX --> {Mars,Pluto,Saturn,Venus}>", 0.91f ,0.81f).en("PlanetX is Mars, Pluto, Saturn, or Venus.");
        n.mustBelieve(cycles, "<planetX --> {Mars,Venus}>", 0.63f ,0.81f).en("PlanetX is either Mars or Venus.");
        n.run();
    }

    @Test
    public void set_operations3() throws InvalidInputException {
        n.believe("<planetX --> [marsy,earthly,venusy]>",1.0f,0.9f).en("PlanetX is Mars, Pluto, or Venus.");
        n.believe("<planetX --> [earthly,saturny]>", 0.1f, 0.9f).en("PlanetX is probably neither Pluto nor Saturn.");
        n.mustBelieve(cycles, "<planetX --> [marsy,earthly,saturny,venusy]>", 0.91f ,0.81f).en("PlanetX is Mars, Pluto, Saturn, or Venus.");
        n.mustBelieve(cycles, "<planetX --> [marsy,venusy]>", 0.63f ,0.81f).en("PlanetX is either Mars or Venus.");
        n.run();
    }

    @Test
    public void set_operations4() throws InvalidInputException {
        n.believe("<[marsy,earthly,venusy] --> planetX>",1.0f,0.9f).en("PlanetX is Mars, Pluto, or Venus.");
        n.believe("<[earthly,saturny] --> planetX>", 0.1f, 0.9f).en("PlanetX is probably neither Pluto nor Saturn.");
        n.mustBelieve(cycles, "<[marsy,earthly,saturny,venusy] --> planetX>", 0.91f ,0.81f).en("PlanetX is Mars, Pluto, Saturn, or Venus.");
        n.mustBelieve(cycles, "<[marsy,venusy] --> planetX>", 0.63f ,0.81f).en("PlanetX is either Mars or Venus.");
        n.run();
    }

    @Test
    public void set_operations5() throws InvalidInputException {
        n.believe("<{Mars,Pluto,Venus} --> planetX>",1.0f,0.9f).en("PlanetX is Mars, Pluto, or Venus.");
        n.believe("<{Pluto,Saturn} --> planetX>", 0.1f, 0.9f).en("PlanetX is probably neither Pluto nor Saturn.");
        n.mustBelieve(cycles, "<{Mars,Pluto,Saturn,Venus} --> planetX>", 0.91f ,0.81f).en("PlanetX is Mars, Pluto, Saturn, or Venus.");
        //n.mustBelieve(cycles, "<{Mars,Venus} --> planetX>", 0.63f ,0.81f).en("PlanetX is either Mars or Venus.");
        n.run();
    }

    @Test
    public void composition_on_both_sides_of_a_statement() throws InvalidInputException {
        n.believe("<bird --> animal>",0.9f,0.9f).en("Bird is a type of animal.");
        n.ask("<(&,bird,swimmer) --> (&,animal,swimmer)>").en("Is a swimming bird a type of swimming animal?");
        n.mustBelieve(cycles, "<(&,bird,swimmer) --> (&,animal,swimmer)>", 0.90f ,0.73f).en("A swimming bird is probably a type of swimming animal.");
        n.run();
    }

    @Test
    public void composition_on_both_sides_of_a_statement2() throws InvalidInputException {
        n.believe("<bird --> animal>",0.9f,0.9f).en("Bird is a type of animal.");
        n.ask("<(-,swimmer,animal) --> (-,swimmer,bird)>").en("Is a nonanimal swimmer a type of a nonbird swimmer?");
        n.mustBelieve(cycles, "<(-,swimmer,animal) --> (-,swimmer,bird)>", 0.90f ,0.73f).en("A nonanimal swimmer is probably a type of nonbird swimmer.");
        n.run();
    }

    @Test
    public void compound_composition_one_premise() throws InvalidInputException {
        n.believe("<swan --> bird>",0.9f,0.9f).en("Swan is a type of bird.");
        n.ask("<swan --> (|,bird,swimmer)>").en("Is a swan a type of bird or swimmer?");
        n.mustBelieve(cycles, "<swan --> (|,bird,swimmer)>", 0.90f ,0.73f).en("A swan is probably a type of bird or swimmer.");
        n.run();
    }

    @Test
     public void compound_composition_one_premise2() throws InvalidInputException {
        n.believe("<swan --> bird>",0.9f,0.9f).en("Swan is a type of bird.");
        n.ask("<(&,swan,swimmer) --> bird>").en("Is swimming swan a type of bird?");
        n.mustBelieve(cycles, "<(&,swan,swimmer) --> bird>", 0.90f ,0.73f).en("Swimming swan is a type of bird.");
        n.run();
    }

    @Test
    public void compound_composition_one_premise3() throws InvalidInputException {
        n.believe("<swan --> bird>",0.9f,0.9f).en("Swan is a type of bird.");
        n.ask("<swan --> (-,swimmer,bird)>").en("Is swan a type of nonbird swimmer?");
        n.mustBelieve(cycles, "<swan --> (-,swimmer,bird)>", 0.10f ,0.73f).en("A swan is not a type of nonbird swimmer.");
        n.run();
    }

    @Test
    public void compound_composition_one_premise4() throws InvalidInputException {
        n.believe("<swan --> bird>",0.9f,0.9f).en("Swan is a type of bird.");
        n.ask("<(~,swimmer, swan) --> bird>").en("Is being bird what differ swimmer from swan?");
        n.mustBelieve(cycles, "<(~,swimmer, swan) --> bird>", 0.10f, 0.73f).en("What differs swimmer from swan is not being bird.");
        n.run();
    }

    @Test
    public void compound_decomposition_one_premise() throws InvalidInputException {
        n.believe("<robin --> (-,bird,swimmer)>", 0.9f, 0.9f).en("Robin is a type of nonswimming bird.");
        n.mustBelieve(cycles, "<robin --> bird>", 0.90f ,0.73f).en("Robin is a type of bird.");
        n.run();
    }

    @Test
    public void compound_decomposition_one_premise2() throws InvalidInputException {
        n.believe("<(|, boy, girl) --> youth>", 0.9f, 0.9f).en("Boys and gials are youth.");
        n.mustBelieve(cycles, "<boy --> youth>", 0.90f ,0.73f).en("Boys are youth.");
        n.run();
    }

    @Test
    public void compound_decomposition_one_premise3() throws InvalidInputException {
        n.believe("<(~, boy, girl) --> [strong]>", 0.9f, 0.9f).en("What differs boys from girls are being strong.");
        n.mustBelieve(cycles, "<boy --> [strong]>", 0.90f ,0.73f).en("Boys are strong.");
        n.run();
    }
}

