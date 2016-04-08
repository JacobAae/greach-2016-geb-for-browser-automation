/**
 * This is Cedric Champeau's solver using Geb, patched, to work after the game was updated
 * Original version can be found at https://gist.github.com/melix/9619800
 */

@Grapes([
@Grab("org.gebish:geb-core:0.9.2"),
@Grab("org.seleniumhq.selenium:selenium-chrome-driver:2.33.0"),
@Grab("org.seleniumhq.selenium:selenium-support:2.31.0")
])
import geb.Browser
import groovy.transform.EqualsAndHashCode
import groovy.transform.InheritConstructors
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.Keys
import groovy.transform.CompileStatic
import groovy.transform.Field
import groovy.transform.Immutable
import groovy.transform.ToString

import java.util.concurrent.Executors

System.setProperty("webdriver.chrome.driver", "/home/jacob/repositories/gr8conf/gr8conf-in-2016-geb-for-grails/chromedriver");

@Field Browser browser

@Field int trials = 0
@Field int success = 0
@Field List<Integer> scores = []

// to reduce computation time
@Field final int gridSize = 4
@Field final int squareGridSize = gridSize*gridSize

void restart()  {
    if (trials) {
        scores << (browser.js.'window.score' as Integer)
        println "Success / Trials: $success / $trials"
        println "Success rate: ${100*((double)success/(double)trials)}%"
        println "Average score: ${(((int)scores.sum())/trials)} ($scores)"
    }
    browser.js.exec 'document.getElementsByClassName(\'retry-button\')[0].click();'
    trials++
}

@CompileStatic
@ToString
class Tuple<T,U> {
    T left
    U right

    Tuple(final T left, final U right) {
        this.left = left
        this.right = right
    }
}

@CompileStatic
@InheritConstructors
class ScoreAndLine extends Tuple<Double, int[]> {}

@CompileStatic
class ScoreAndGrid extends Tuple<Double, int[][]> {
    final int freeCells
    final boolean win

    ScoreAndGrid(double score, int[][] grid) {
        super(score, grid)
        boolean hasGoal = false
        int sum = 0;
        for (int i=0; i<grid.length; i++) {
            int[] row = grid[i];
            for (int j=0;j<row.length;j++) {
                if (row[j]==0) {
                    sum++
                } else if (row[j]==2048) {
                    hasGoal = true
                }
            }
        }
        freeCells = sum
        win = hasGoal
    }
}

@CompileStatic
@InheritConstructors
class IndexAndRow extends Tuple<Integer, Integer> {}

@CompileStatic
@InheritConstructors
class ScoreAndGridCouple extends Tuple<ScoreAndGrid, ScoreAndGrid> {}

@CompileStatic
@InheritConstructors
class ScoreAndDirection extends Tuple<Double, Integer> implements Comparable<ScoreAndDirection> {
    @Override
    int compareTo(final ScoreAndDirection o) {
        o.left <=> ((double)left)
    }
}

// given a row, computes the score of a single line merge
@CompileStatic
ScoreAndLine move(int[] source) {
    int len = source.length
    int[] res = new int[len]
    int idx = 0
    for (int i=0;i< len;i++) {
        if (source[i]!=0) {
            res[idx++] = source[i];
        }
    }
    int start = 0
    int next = 0
    double score = 0d
    while (start < len) {
        def ps = next>gridSize-1?0:res[next]
        def pv = next>gridSize-2?0:res[next + 1]
        if (ps == pv) {
            res[start] = 2 * ps
            score += 4*ps*ps
            next++
        } else {
            res[start] = ps
        }
        next++
        start++
    }

    boolean noChange = Arrays.equals(source, res)
    new ScoreAndLine(noChange?-1d:score, (int[])res)
}

// adjusts the score for a full grid. A move where nothing changed is considered a bad move
// while a move which triggered lots of merges gains bonus
@CompileStatic
ScoreAndGrid computeGridScore(ScoreAndLine[] scoreAndLines, ScoreAndGrid grid) {
    int[][] src = grid.right
    int noChange = 0
    // a grid without changes counts as bad move (-1)
    double score = 0.0d
    int len = src.length
    int[][] lines = new int[len][]
    for (int i = 0; i < len; i++) {
        ScoreAndLine line = scoreAndLines[i]
        double lineScore = line.left
        if (lineScore==-1d) {
            noChange++
        } else {
            score += lineScore.doubleValue()
        }
        lines[i] = line.right;
    }

    def result
    if (noChange==len) {
        result = new ScoreAndGrid(-1d, lines)
    } else {
        result = new ScoreAndGrid(score, lines)
        // if a move frees up a lot of space, it is in general better
        // because free space gives us ability to make the fusions easier
        score = Math.pow(score, Math.sqrt(1+result.freeCells))
        result.left = score
    }

    result
}

@CompileStatic
int[] reverse(int[] arr) {
    int len = arr.length
    int[] result = new int[len]
    for (int i=0; i< len;i++) {
       result[len-i-1] = arr[i]
    }
    result
}

@CompileStatic
int[][] transpose(int[][] grid) {
    int len = gridSize
    int[][] res = new int[len][]
    for (int i=0; i<len;i++) {
        res[i] = new int[len]
        for (int j=0; j<len;j++) {
            res[i][j] = grid[j][i];
        }
    }
    res
}

// computes the score of a move on a full grid
@CompileStatic
ScoreAndGridCouple leftRightScore(int[][] src) {
    ScoreAndLine[] left = new ScoreAndLine[src.length]
    for (int i = 0; i < src.length; i++) {
        left[i] = move(src[i]);
    }

    ScoreAndLine[] right = new ScoreAndLine[src.length]
    for (int i = 0; i < src.length; i++) {
        def item = move(reverse(src[i]))
        item.right = reverse((int[])item.right) // cast shouldn't be necessary...
        right[i] = item;
    }

    // adjust
    def grid = new ScoreAndGrid(0, src)
    ScoreAndGridCouple result = new ScoreAndGridCouple(computeGridScore(left, grid), computeGridScore(right, grid))
    result
}

// when the real game occurs, a new random tile is added
// this tries to emulate this behavior in order to improve
// accuracy of predictions. We can't explore the full tree
// so we only choose *one* random cell.
@CompileStatic
IndexAndRow chooseRandomCell(int[][] grid) {
    int len = gridSize
    List<IndexAndRow> emptyCells = []
    for (int i=0; i<len;i++) {
        for (int j=0; j<len;j++) {
            if (grid[i][j]==0) {
                emptyCells.add new IndexAndRow(i, j)
            }
        }
    }
    if (!emptyCells.empty) {
        Collections.shuffle(emptyCells)
        return emptyCells[0]
    }
    return null
}

// This is used to tweak the score in case further moves will later
// allow us to merge with a higher score.
// Of course, since we don't know which tiles will appear after each move
// the deeper we go, the lower the score is because the prediction becomes
// unreliable

@CompileStatic
double childScore(ScoreAndGrid child, int depth, int maxDepth) {
    double subScore = child.left.doubleValue()
    int[][] grid = child.right
    if (child.win) {
        // seriously boost
        // but pay care because it's an estimate!
        return Math.pow(subScore,1+depth)
    }

    // the idea for the sqrt here is that if there are less free cells, there's a higher probability that the
    // prediction is correct
    double estimate = estimateScore(grid, depth - 1, maxDepth)
    if (estimate<0d) {
        // ass-saving: avoid taking bad decisions if we detect a dead end
        return subScore/Math.pow(2d,1+depth)
    }
    double score = (subScore + estimate)/ Math.sqrt(1 + child.freeCells)
    score
}

@CompileStatic
double estimateScore(int[][] grid, int depth, int maxDepth) {
    double score = 0d
    if (depth > 0) {
        IndexAndRow indexAndRow = chooseRandomCell(grid)
        int index = indexAndRow.left
        int row = indexAndRow.right
        grid[index][row] = 2;
        double score2 = 0.9d*recurseEstimate(grid, depth, maxDepth)
        grid[index][row] = 4;
        double score4 = 0.1d*recurseEstimate(grid, depth, maxDepth)
        score = (score2 + score4)*0.5d
    }
    score
}

@CompileStatic
private double recurseEstimate(int[][] grid, int depth, int maxDepth) {
    ScoreAndGridCouple leftright = leftRightScore(grid)
    ScoreAndGridCouple updown = leftRightScore(transpose(grid))
    ScoreAndGrid left = leftright.left
    ScoreAndGrid right = leftright.right
    ScoreAndGrid up = updown.left
    ScoreAndGrid down = updown.right
    double leftScore = left.left.doubleValue()
    double rightScore = right.left.doubleValue()
    double upScore = up.left.doubleValue()
    double downScore = down.left.doubleValue()
    double score = 0d
    if (leftScore==-1d && rightScore==-1d && upScore==-1d && downScore==-1d) {
        return -1d;
    }
    if (leftScore >=0d) {
        score += childScore(left, depth, maxDepth)
    }
    if (rightScore >=0d) {
        score += childScore(right, depth, maxDepth)
    }
    if (upScore >= 0d) {
        score += childScore(up, depth, maxDepth)
    }
    if (downScore >=0d) {
        score += childScore(down, depth, maxDepth)
    }
    score
}

def nextMove(List<List<Integer>> orig) {
    int[][] grid = orig as int[][]
    def root = new ScoreAndGrid(0, grid)
    if (root.win) {
        return true
    }
    // adaptative depth exploration. It is more important to take care when there's no a lot of free space
    int depth = 1 + 2*((int)Math.sqrt(0.5d*(squareGridSize-root.freeCells)))
    //println "Depth $depth"
    ScoreAndGridCouple leftright = leftRightScore(grid)
    ScoreAndGridCouple updown = leftRightScore(transpose(grid))
    ScoreAndGrid leftScoreAndGrid = leftright.left
    ScoreAndGrid rightScoreAndGrid = leftright.right
    ScoreAndGrid upScoreAndGrid = updown.left
    ScoreAndGrid downScoreAndGrid = updown.right
    if (leftScoreAndGrid.left==-1d && rightScoreAndGrid.left==-1d && upScoreAndGrid.left==-1d && downScoreAndGrid.left == -1d) {
        sleep(5000)
        // game over !
        restart()
    } else {
        double left = leftScoreAndGrid.left>=0d?leftScoreAndGrid.left + estimateScore(leftScoreAndGrid.right, depth, depth):-1d
        double right = rightScoreAndGrid.left>=0d?rightScoreAndGrid.left + estimateScore(rightScoreAndGrid.right, depth, depth):-1d
        double up = upScoreAndGrid.left>=0d?upScoreAndGrid.left + estimateScore(upScoreAndGrid.right, depth, depth):-1d
        double down = downScoreAndGrid.left>=0d?downScoreAndGrid.left + estimateScore(downScoreAndGrid.right, depth, depth):-1d
        //println "Score for (left, right, up, down) : ($left,$right,$up,$down)"

        def solutions = [
                new ScoreAndDirection(left, 37),
                new ScoreAndDirection(up, 38),
                new ScoreAndDirection(right, 39),
                new ScoreAndDirection(down, 40)]
        Collections.shuffle(solutions)
        Collections.sort(solutions)
        int keycode = (int) solutions[0].right
        browser.js.exec keycode, """
    var keyEvent = document.createEvent("Events");
    var keyCode = arguments[0];
    keyEvent.initEvent("keydown", true, true);
    keyEvent.keyCode = keyCode;
    keyEvent.which = keyCode;
    document.dispatchEvent(keyEvent);"""
    }
    return false
}



browser = new Browser(driver: new ChromeDriver())

browser.with {
    go "http://gabrielecirulli.github.io/2048/"

    js.exec '''(function() { 

GameManager.prototype.export = function() {
   window.grid = Array(this.grid.size);
   window.score = this.score;
   for (var i = 0; i < this.grid.size; i++) {
    window.grid[i] = new Array(this.grid.size);
   }
   this.grid.eachCell(function(x,y,z) {        
	window.grid[y][x] = z==null||z==undefined?0:z.value;  
   });
};
GameManager.prototype.oldSetup = GameManager.prototype.setup;
GameManager.prototype.setup = function() {
    this.oldSetup();
    this.export();    
};
GameManager.prototype.origActuate = GameManager.prototype.actuate;
GameManager.prototype.actuate = function() {
   this.export();
   this.origActuate();
}

})();

// This method is giving problems, so we overwrite it
KeyboardInputManager.prototype.targetIsInput = function (event) {
  return false;
};
'''

    restart()

    while (true) {
        sleep(80)
        if (nextMove(js.'window.grid')) {
            gameWin()
        }
    }
}

private void gameWin() {
    success++
    sleep(5000)
    restart()
}
