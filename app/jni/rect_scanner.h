//
//  fm_ocr_scanner.hpp
//  FMHEDNet
//
//  Created by fengjian on 2018/4/11.
//  Copyright © 2018年 fengjian. All rights reserved.
//
//  https://github.com/fengjian0106/hed-tutorial-for-document-scanning
//

#ifndef fm_ocr_scanner_hpp
#define fm_ocr_scanner_hpp

#include <stdio.h>
#include <array>
#include <tuple>
#include <opencv2/opencv.hpp>
#include <opencv2/core/core.hpp>

namespace scanner {
    class RectDetect {
    public:
        RectDetect();
        virtual ~RectDetect();
        std::vector<cv::Point> processEdgeImage(cv::Mat edge_image);

    private:

    };

}


#endif /* fm_ocr_scanner_hpp */
