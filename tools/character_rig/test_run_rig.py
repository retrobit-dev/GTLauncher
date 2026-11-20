import math
import unittest
from run_rig import joints, ANCHORS

class RunRigTest(unittest.TestCase):
    def test_fixed_segment_lengths_and_anchors(self):
        for view in ANCHORS:
            for i in range(12):
                pose = joints(view, 2*math.pi*i/12)
                for side in ('near','far'):
                    limb = pose[side]
                    for a,b,length in [('hip','knee',43),('knee','ankle',38),('shoulder','elbow',29),('elbow','wrist',24)]:
                        self.assertAlmostEqual(math.dist(limb[a],limb[b]),length,delta=.002)
                    self.assertEqual(limb['hip'],list(ANCHORS[view][side+'_hip']))
    def test_opposite_steps_and_closed_cycle(self):
        for view in ANCHORS:
            a,b=joints(view,math.pi/2),joints(view,3*math.pi/2)
            forward=-1 if view=='front' else 1
            self.assertGreater(forward*(a['near']['knee'][0]-a['near']['hip'][0]),0)
            self.assertLess(forward*(b['near']['knee'][0]-b['near']['hip'][0]),0)
            self.assertLess(forward*(a['near']['elbow'][0]-a['near']['shoulder'][0]),0)
            self.assertEqual(joints(view,0),joints(view,2*math.pi))

if __name__ == '__main__': unittest.main()
